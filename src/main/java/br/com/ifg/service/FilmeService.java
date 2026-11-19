// FilmeService.java
package br.com.ifg.service;

import br.com.ifg.model.dao.FilmeDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.FilmeRequestDTO;
import br.com.ifg.model.dto.FilmeResponseDTO;
import br.com.ifg.model.entity.Filme;
import br.com.ifg.model.entity.Plano;
import br.com.ifg.model.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;

@ApplicationScoped
public class FilmeService {

    @Inject
    FilmeDAO filmeDAO;

    @Inject
    UsuarioDAO usuarioDAO;

    @Inject
    JsonWebToken jwt;

    public List<FilmeResponseDTO> buscarFilmesPorPlanoDoUsuarioLogado() {
        Usuario usuario = obterUsuarioLogado();

        if (Boolean.TRUE.equals(usuario.getIsAdmin())) {
            return listarTodos();
        }

        if (usuario.getPlano() == null) {
            return List.of();
        }

        return filmeDAO.buscarFilmesPorNivelPlano(usuario.getPlano().getNivel())
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    public FilmeResponseDTO buscarPorIdParaUsuarioLogado(Long id) {
        Usuario usuario = obterUsuarioLogado();
        Filme filme = buscarPorId(id);

        if (!Boolean.TRUE.equals(usuario.getIsAdmin()) && filme.getPlano() != null) {
            if (usuario.getPlano() == null || filme.getPlano().getNivel() > usuario.getPlano().getNivel()) {
                throw new SecurityException("O seu plano atual não permite assistir a este filme.");
            }
        }
        return converterParaDTO(filme);
    }

    public List<FilmeResponseDTO> listarTodos() {
        return filmeDAO.listarTodosOrdenados().stream()
                .map(this::converterParaDTO)
                .toList();
    }

    public Filme buscarPorId(Long id) {
        Filme filme = filmeDAO.findById(id);
        if (filme == null) {
            throw new RuntimeException("Filme não encontrado.");
        }
        return filme;
    }

    // FilmeService.java (métodos cadastrar e atualizar usando FilmeRequestDTO)
    @Transactional
    public FilmeResponseDTO cadastrar(FilmeRequestDTO dto) {
        Plano planoGerenciado = filmeDAO.getEntityManager().getReference(Plano.class, dto.planoId());

        Filme filme = new Filme();
        filme.setTitulo(dto.titulo());
        filme.setDescricao(dto.descricao());
        filme.setGenero(dto.genero());
        filme.setCaminhoImagemHorizontal(dto.caminhoImagemHorizontal());
        filme.setCaminhoImagemVertical(dto.caminhoImagemVertical());
        filme.setPlano(planoGerenciado);

        filmeDAO.persist(filme);
        return converterParaDTO(filme);
    }

    @Transactional
    public FilmeResponseDTO atualizar(Long id, FilmeRequestDTO dto) {
        Filme filme = buscarPorId(id);
        Plano planoGerenciado = filmeDAO.getEntityManager().getReference(Plano.class, dto.planoId());

        filme.setTitulo(dto.titulo());
        filme.setDescricao(dto.descricao());
        filme.setGenero(dto.genero());
        filme.setCaminhoImagemHorizontal(dto.caminhoImagemHorizontal());
        filme.setCaminhoImagemVertical(dto.caminhoImagemVertical());
        filme.setPlano(planoGerenciado);

        return converterParaDTO(filme);
    }

    @Transactional
    public void deletar(Long id) {
        Filme filme = buscarPorId(id);
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

    private FilmeResponseDTO converterParaDTO(Filme filme) {
        return new FilmeResponseDTO(
                filme.getId(),
                filme.getTitulo(),
                filme.getDescricao(),
                filme.getGenero(),
                filme.getCaminhoImagemHorizontal(),
                filme.getCaminhoImagemVertical(),
                filme.getPlano() != null ? filme.getPlano().getId() : null
        );
    }
}