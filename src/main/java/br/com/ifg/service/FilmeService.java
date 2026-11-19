package br.com.ifg.service;

import br.com.ifg.model.bo.FilmeBO;
import br.com.ifg.model.dao.FilmeDAO;
import br.com.ifg.model.dao.PlanoDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.CatalogoHomeResponseDTO;
import br.com.ifg.model.dto.FilmeRequestDTO;
import br.com.ifg.model.dto.FilmeResponseDTO;
import br.com.ifg.model.dto.InteracaoFilmeResponseDTO;
import br.com.ifg.model.entity.Filme;
import br.com.ifg.model.entity.Plano;
import br.com.ifg.model.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ApplicationScoped
public class FilmeService {

    private static final Logger LOG = Logger.getLogger(FilmeService.class);
    private static final String PREFIXO_ROTA_HORIZONTAL = "/filmes/imagem/horizontal/";
    private static final String PREFIXO_ROTA_VERTICAL = "/filmes/imagem/vertical/";

    @Inject
    FilmeDAO filmeDAO;

    @Inject
    PlanoDAO planoDAO;

    @Inject
    UsuarioDAO usuarioDAO;

    @Inject
    FilmeBO filmeBO;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "mediaflix.upload.filmes.horizontal.dir", defaultValue = "uploads/filmes/horizontal")
    String diretorioHorizontal;

    @ConfigProperty(name = "mediaflix.upload.filmes.vertical.dir", defaultValue = "uploads/filmes/vertical")
    String diretorioVertical;

    private List<Filme> listarEntidadesPermitidas(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getIsAdmin())) {
            return filmeDAO.listarTodosOrdenados();
        }
        if (usuario.getPlano() == null) {
            return List.of();
        }
        return filmeDAO.buscarFilmesPorNivelPlano(usuario.getPlano().getNivel());
    }

    public List<FilmeResponseDTO> buscarFilmesPorPlanoDoUsuarioLogado() {
        Usuario usuario = obterUsuarioLogado();
        return listarEntidadesPermitidas(usuario).stream()
                .map(filmeBO::converterParaDTO)
                .toList();
    }

    @Transactional
    public CatalogoHomeResponseDTO montarCatalogoHome() {
        Usuario usuario = obterUsuarioLogado();
        List<Filme> permitidos = listarEntidadesPermitidas(usuario);
        return filmeBO.organizarCatalogoHome(
                permitidos,
                usuario.getMinhaLista(),
                usuario.getFilmesCurtidos()
        );
    }

    @Transactional
    public InteracaoFilmeResponseDTO consultarStatusInteracao(Long filmeId) {
        Usuario usuario = obterUsuarioLogado();
        Filme filme = buscarPorId(filmeId);
        filmeBO.validarAcessoAoFilme(usuario, filme);

        boolean naLista = usuario.getMinhaLista().contains(filme);
        boolean curtido = usuario.getFilmesCurtidos().contains(filme);
        return new InteracaoFilmeResponseDTO(filmeId, naLista, curtido);
    }

    @Transactional
    public InteracaoFilmeResponseDTO alternarMinhaLista(Long filmeId) {
        Usuario usuario = obterUsuarioLogado();
        Filme filme = buscarPorId(filmeId);
        filmeBO.validarAcessoAoFilme(usuario, filme);

        if (usuario.getMinhaLista().contains(filme)) {
            usuario.getMinhaLista().remove(filme);
        } else {
            usuario.getMinhaLista().add(filme);
        }

        boolean naLista = usuario.getMinhaLista().contains(filme);
        boolean curtido = usuario.getFilmesCurtidos().contains(filme);
        return new InteracaoFilmeResponseDTO(filmeId, naLista, curtido);
    }

    @Transactional
    public InteracaoFilmeResponseDTO alternarCurtida(Long filmeId) {
        Usuario usuario = obterUsuarioLogado();
        Filme filme = buscarPorId(filmeId);
        filmeBO.validarAcessoAoFilme(usuario, filme);

        if (usuario.getFilmesCurtidos().contains(filme)) {
            usuario.getFilmesCurtidos().remove(filme);
        } else {
            usuario.getFilmesCurtidos().add(filme);
        }

        boolean naLista = usuario.getMinhaLista().contains(filme);
        boolean curtido = usuario.getFilmesCurtidos().contains(filme);
        return new InteracaoFilmeResponseDTO(filmeId, naLista, curtido);
    }

    public FilmeResponseDTO buscarPorIdParaUsuarioLogado(Long id) {
        Usuario usuario = obterUsuarioLogado();
        Filme filme = buscarPorId(id);
        filmeBO.validarAcessoAoFilme(usuario, filme);
        return filmeBO.converterParaDTO(filme);
    }

    public List<FilmeResponseDTO> listarTodos() {
        return filmeDAO.listarTodosOrdenados().stream()
                .map(filmeBO::converterParaDTO)
                .toList();
    }

    public Filme buscarPorId(Long id) {
        Filme filme = filmeDAO.findById(id);
        if (filme == null) {
            throw new NotFoundException("Obra não encontrada.");
        }
        return filme;
    }

    @Transactional
    public FilmeResponseDTO cadastrar(FilmeRequestDTO dto) {
        Plano plano = filmeBO.validarPlanoExistente(planoDAO.findById(dto.planoId()));
        String tipoValidado = filmeBO.validarENormalizarTipo(dto.tipo());
        String generosFormatados = filmeBO.validarEFormatarGeneros(dto.generos());

        Filme filme = new Filme();
        filme.setTitulo(dto.titulo());
        filme.setDescricao(dto.descricao());
        filme.setTipo(tipoValidado);
        filme.setGenero(generosFormatados);
        filme.setCaminhoImagemHorizontal(dto.caminhoImagemHorizontal());
        filme.setCaminhoImagemVertical(dto.caminhoImagemVertical());
        filme.setPlano(plano);

        filmeDAO.persist(filme);
        return filmeBO.converterParaDTO(filme);
    }

    @Transactional
    public FilmeResponseDTO atualizar(Long id, FilmeRequestDTO dto) {
        Filme filme = buscarPorId(id);
        Plano plano = filmeBO.validarPlanoExistente(planoDAO.findById(dto.planoId()));
        String tipoValidado = filmeBO.validarENormalizarTipo(dto.tipo());
        String generosFormatados = filmeBO.validarEFormatarGeneros(dto.generos());

        filme.setTitulo(dto.titulo());
        filme.setDescricao(dto.descricao());
        filme.setTipo(tipoValidado);
        filme.setGenero(generosFormatados);
        filme.setPlano(plano);

        if (dto.caminhoImagemHorizontal() != null && !dto.caminhoImagemHorizontal().isBlank()) {
            filme.setCaminhoImagemHorizontal(dto.caminhoImagemHorizontal());
        }
        if (dto.caminhoImagemVertical() != null && !dto.caminhoImagemVertical().isBlank()) {
            filme.setCaminhoImagemVertical(dto.caminhoImagemVertical());
        }

        return filmeBO.converterParaDTO(filme);
    }

    @Transactional
    public FilmeResponseDTO salvarImagensFilme(Long id, FileUpload imagemHorizontal, FileUpload imagemVertical) {
        Filme filme = buscarPorId(id);

        try {
            if (imagemHorizontal != null && imagemHorizontal.size() > 0) {
                Path pastaHoriz = Paths.get(diretorioHorizontal).toAbsolutePath().normalize();
                String urlHoriz = filmeBO.salvarImagemNoDisco(
                        imagemHorizontal,
                        filme.getTitulo(),
                        pastaHoriz,
                        filme.getCaminhoImagemHorizontal(),
                        PREFIXO_ROTA_HORIZONTAL
                );
                filme.setCaminhoImagemHorizontal(urlHoriz);
                LOG.infof("Imagem horizontal salva para obra ID %d -> %s", filme.getId(), urlHoriz);
            }

            if (imagemVertical != null && imagemVertical.size() > 0) {
                Path pastaVert = Paths.get(diretorioVertical).toAbsolutePath().normalize();
                String urlVert = filmeBO.salvarImagemNoDisco(
                        imagemVertical,
                        filme.getTitulo(),
                        pastaVert,
                        filme.getCaminhoImagemVertical(),
                        PREFIXO_ROTA_VERTICAL
                );
                filme.setCaminhoImagemVertical(urlVert);
                LOG.infof("Imagem vertical salva para obra ID %d -> %s", filme.getId(), urlVert);
            }

            return filmeBO.converterParaDTO(filme);

        } catch (IOException e) {
            LOG.error("Erro ao gravar imagens da obra no servidor", e);
            throw new RuntimeException("Erro ao salvar as imagens no servidor.");
        }
    }

    public Path obterCaminhoImagem(String tipo, String nomeArquivo) {
        String baseDir = switch (tipo.toLowerCase()) {
            case "horizontal" -> diretorioHorizontal;
            case "vertical" -> diretorioVertical;
            default -> throw new IllegalArgumentException("Tipo de imagem inválido.");
        };

        Path pastaDestino = Paths.get(baseDir).toAbsolutePath().normalize();
        Path arquivo = pastaDestino.resolve(nomeArquivo).normalize();

        if (!arquivo.startsWith(pastaDestino) || !Files.exists(arquivo)) {
            throw new IllegalArgumentException("Imagem não encontrada.");
        }
        return arquivo;
    }

    @Transactional
    public void deletar(Long id) {
        Filme filme = buscarPorId(id);
        filmeDAO.removerVinculosDoFilme(id);
        filmeBO.removerArquivoSeLocal(
                filme.getCaminhoImagemHorizontal(),
                Paths.get(diretorioHorizontal).toAbsolutePath().normalize(),
                PREFIXO_ROTA_HORIZONTAL
        );
        filmeBO.removerArquivoSeLocal(
                filme.getCaminhoImagemVertical(),
                Paths.get(diretorioVertical).toAbsolutePath().normalize(),
                PREFIXO_ROTA_VERTICAL
        );
        filmeDAO.delete(filme);
    }

    private Usuario obterUsuarioLogado() {
        String email = jwt.getName();
        if (email == null) {
            throw new SecurityException("Token inválido ou ausente.");
        }
        Usuario usuario = usuarioDAO.buscarPorEmail(email);
        if (usuario == null) {
            throw new RuntimeException("Utilizador não encontrado.");
        }
        return usuario;
    }
}