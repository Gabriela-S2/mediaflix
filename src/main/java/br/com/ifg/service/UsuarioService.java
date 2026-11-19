package br.com.ifg.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import br.com.ifg.model.dao.PlanoDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.PlanoResponseDTO;
import br.com.ifg.model.dto.UsuarioRequestDTO;
import br.com.ifg.model.dto.UsuarioResponseDTO;
import br.com.ifg.model.entity.Plano;
import br.com.ifg.model.entity.Usuario;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class UsuarioService {

    @Inject
    UsuarioDAO usuarioDAO;

    @Inject
    PlanoDAO planoDAO;

    private static final String FOTO_PADRAO = "/midia/novo_usuario.png";
    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(UsuarioService.class);
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");

    @ConfigProperty(name = "mediaflix.upload.dir", defaultValue = "uploads/perfis")
    String diretorioUpload;


    @Transactional
    public UsuarioResponseDTO salvarFotoPerfil(String emailLogado, FileUpload arquivo) {
        if (arquivo == null || arquivo.uploadedFile() == null || arquivo.size() == 0) {
            throw new IllegalArgumentException("Nenhuma imagem foi enviada.");
        }

        String contentType = arquivo.contentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato inválido. Envie apenas PNG, JPG ou WEBP.");
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(emailLogado);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        try {
            Path pastaDestino = Paths.get(diretorioUpload).toAbsolutePath().normalize();
            Files.createDirectories(pastaDestino);

            // Gera o nome no padrão imutável: <id>_<data>.png
            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String nomeArquivo = usuario.getId() + "_" + dataAtual + ".png";
            Path caminhoCompleto = pastaDestino.resolve(nomeArquivo).normalize();

            // Converte e grava a imagem em formato PNG real
            BufferedImage imagem = ImageIO.read(arquivo.uploadedFile().toFile());
            if (imagem == null) {
                throw new IllegalArgumentException("O arquivo enviado não é uma imagem válida.");
            }
            ImageIO.write(imagem, "png", caminhoCompleto.toFile());

            // Remove a foto anterior do disco se tiver sido salva em outra data
            if (usuario.getCaminhoFoto() != null && usuario.getCaminhoFoto().startsWith("/usuarios/foto/")) {
                String arquivoAnterior = usuario.getCaminhoFoto().replace("/usuarios/foto/", "");
                if (!arquivoAnterior.equals(nomeArquivo)) {
                    Path caminhoAnterior = pastaDestino.resolve(arquivoAnterior).normalize();
                    if (caminhoAnterior.startsWith(pastaDestino)) {
                        Files.deleteIfExists(caminhoAnterior);
                    }
                }
            }

            // Atualiza a rota pública na entidade Usuario
            String urlPublica = "/usuarios/foto/" + nomeArquivo;
            usuario.setCaminhoFoto(urlPublica);

            LOG.infof("Foto de perfil atualizada para usuário ID %d -> %s", usuario.getId(), caminhoCompleto);
            return converterParaDTO(usuario);

        } catch (IOException e) {
            LOG.error("Falha ao gravar foto de perfil no disco", e);
            throw new RuntimeException("Erro ao salvar a imagem no servidor.");
        }
    }

    public Path obterCaminhoFoto(String nomeArquivo) {
        Path pastaDestino = Paths.get(diretorioUpload).toAbsolutePath().normalize();
        Path arquivo = pastaDestino.resolve(nomeArquivo).normalize();

        // Proteção contra Path Traversal (../)
        if (!arquivo.startsWith(pastaDestino) || !Files.exists(arquivo)) {
            throw new IllegalArgumentException("Foto de perfil não encontrada.");
        }
        return arquivo;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        if (usuarioDAO.buscarPorEmail(dto.email()) != null) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        if (usuarioDAO.buscarPorUsername(dto.username()) != null) {
            throw new IllegalArgumentException("Este nome de usuário já está em uso.");
        }

        Plano plano = planoDAO.findById(dto.planoId());
        if (plano == null) {
            throw new IllegalArgumentException("Plano selecionado inválido.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());
        usuario.setSenha(BcryptUtil.bcryptHash(dto.senha()));
        usuario.setCaminhoFoto(FOTO_PADRAO);
        usuario.setIsAdmin(false);
        usuario.setPlano(plano);

        usuarioDAO.persist(usuario);
        return converterParaDTO(usuario);
    }

    public String autenticar(String email, String senha) {
        Usuario usuario = usuarioDAO.buscarPorEmail(email);
        if (usuario == null || !BcryptUtil.matches(senha, usuario.getSenha())) {
            throw new SecurityException("E-mail ou senha incorretos.");
        }

        Set<String> grupos = Boolean.TRUE.equals(usuario.getIsAdmin())
                ? Set.of("USER", "ADMIN")
                : Set.of("USER");

        return Jwt.issuer("https://mediaflix.ifg.edu.br")
                .upn(usuario.getEmail())
                .groups(grupos)
                .expiresIn(Duration.ofHours(24))
                .sign();
    }

    private UsuarioResponseDTO converterParaDTO(Usuario usuario) {
        PlanoResponseDTO planoDTO = null;
        if (usuario.getPlano() != null) {
            Plano p = usuario.getPlano();
            planoDTO = new PlanoResponseDTO(p.getId(), p.getNome(), p.getNivel(), p.getTemAnuncios());
        }

        String foto = (usuario.getCaminhoFoto() != null && !usuario.getCaminhoFoto().isBlank())
                ? usuario.getCaminhoFoto()
                : FOTO_PADRAO;

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                foto,
                usuario.getIsAdmin(),
                planoDTO
        );
    }

    public UsuarioResponseDTO buscarPerfil(String email) {
        Usuario usuario = usuarioDAO.buscarPorEmail(email);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        return converterParaDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarPerfil(String emailLogado, br.com.ifg.model.dto.AtualizarPerfilRequestDTO dto) {
        Usuario usuario = usuarioDAO.buscarPorEmail(emailLogado);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        Usuario existenteComUsername = usuarioDAO.buscarPorUsername(dto.username());
        if (existenteComUsername != null && !existenteComUsername.getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Este nome de usuário já está em uso.");
        }

        if (dto.caminhoFoto() != null && !dto.caminhoFoto().isBlank()) {
            usuario.setCaminhoFoto(dto.caminhoFoto());
        }

        Plano plano = planoDAO.findById(dto.planoId());
        if (plano == null) {
            throw new IllegalArgumentException("Plano selecionado inválido.");
        }

        usuario.setUsername(dto.username());
        usuario.setCaminhoFoto(dto.caminhoFoto());
        usuario.setPlano(plano);

        LOG.infof("Perfil atualizado para o usuário: %s (Plano: %s)", emailLogado, plano.getNome());
        return converterParaDTO(usuario);
    }
}