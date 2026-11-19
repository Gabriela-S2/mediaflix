package br.com.ifg.service;

import br.com.ifg.model.bo.UsuarioBO;
import br.com.ifg.model.dao.PlanoDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.AtualizarPerfilRequestDTO;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@ApplicationScoped
public class UsuarioService {

    @Inject
    UsuarioDAO usuarioDAO;

    @Inject
    PlanoDAO planoDAO;

    @Inject
    UsuarioBO usuarioBO;

    private static final String FOTO_PADRAO = "/midia/novo_usuario.png";
    private static final Logger LOG = Logger.getLogger(UsuarioService.class);

    @ConfigProperty(name = "mediaflix.upload.dir", defaultValue = "uploads/perfis")
    String diretorioUpload;

    @Transactional
    public UsuarioResponseDTO salvarFotoPerfil(String emailLogado, FileUpload arquivo) {
        usuarioBO.validarUploadFotoPerfil(arquivo);

        Usuario usuario = usuarioDAO.buscarPorEmail(emailLogado);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        try {
            Path pastaDestino = Paths.get(diretorioUpload).toAbsolutePath().normalize();
            Files.createDirectories(pastaDestino);

            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String nomeArquivo = usuario.getId() + "_" + dataAtual + ".png";
            Path caminhoCompleto = pastaDestino.resolve(nomeArquivo).normalize();

            BufferedImage imagem = ImageIO.read(arquivo.uploadedFile().toFile());
            if (imagem == null) {
                throw new IllegalArgumentException("O arquivo enviado não é uma imagem válida.");
            }
            ImageIO.write(imagem, "png", caminhoCompleto.toFile());

            if (usuario.getCaminhoFoto() != null && usuario.getCaminhoFoto().startsWith("/usuarios/foto/")) {
                String arquivoAnterior = usuario.getCaminhoFoto().replace("/usuarios/foto/", "");
                if (!arquivoAnterior.equals(nomeArquivo)) {
                    Path caminhoAnterior = pastaDestino.resolve(arquivoAnterior).normalize();
                    if (caminhoAnterior.startsWith(pastaDestino)) {
                        Files.deleteIfExists(caminhoAnterior);
                    }
                }
            }

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

        if (!arquivo.startsWith(pastaDestino) || !Files.exists(arquivo)) {
            throw new IllegalArgumentException("Foto de perfil não encontrada.");
        }
        return arquivo;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        boolean emailEmUso = usuarioDAO.buscarPorEmail(dto.email()) != null;
        boolean usernameEmUso = usuarioDAO.buscarPorUsername(dto.username()) != null;
        Plano plano = planoDAO.findById(dto.planoId());

        usuarioBO.validarNovoCadastro(dto, emailEmUso, usernameEmUso, plano);

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
        usuarioBO.validarCredenciais(usuario, senha);

        Set<String> grupos = usuarioBO.resolverGruposJwt(usuario);

        return Jwt.issuer("https://mediaflix.ifg.edu.br")
                .upn(usuario.getEmail())
                .groups(grupos)
                .expiresIn(Duration.ofHours(24))
                .sign();
    }

    public UsuarioResponseDTO buscarPerfil(String email) {
        Usuario usuario = usuarioDAO.buscarPorEmail(email);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        return converterParaDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarPerfil(String emailLogado, AtualizarPerfilRequestDTO dto) {
        Usuario usuario = usuarioDAO.buscarPorEmail(emailLogado);
        Usuario existenteComUsername = usuarioDAO.buscarPorUsername(dto.username());
        Plano plano = planoDAO.findById(dto.planoId());

        usuarioBO.validarEAplicarAtualizacaoPerfil(usuario, dto, existenteComUsername, plano);

        LOG.infof("Perfil atualizado para o usuário: %s (Plano: %s)", emailLogado, plano.getNome());
        return converterParaDTO(usuario);
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
}