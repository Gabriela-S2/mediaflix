package br.com.ifg.service;

import br.com.ifg.model.dao.FilmeDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.FilmeResponseDTO;
import br.com.ifg.model.entity.Filme;
import br.com.ifg.model.entity.Plano;
import br.com.ifg.model.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FilmeService {

    @Inject
    FilmeDAO filmeDAO;

    @Inject
    UsuarioDAO usuarioDAO;

    @Inject
    JsonWebToken jwt;

    public List<FilmeResponseDTO> buscarFilmesPorPlanoDoUsuarioLogado() {
        String email = jwt.getName();
        if (email == null) {
            throw new RuntimeException("Token inválido ou sem identificação de utilizador.");
        }

        Usuario usuario = usuarioDAO.find("email", email).firstResult();
        if (usuario == null) {
            throw new RuntimeException("Utilizador não encontrado na base de dados.");
        }

        List<Filme> filmesDb = filmeDAO.listAll();

        // Filtra os filmes: mostra apenas os filmes sem restrição de plano
        // ou cujo nível do plano seja inferior ou igual ao plano do utilizador.
        return filmesDb.stream()
                .filter(filme -> filme.getPlano() == null ||
                        (usuario.getPlano() != null && filme.getPlano().getId() <= usuario.getPlano().getId()))
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public List<FilmeResponseDTO> listarTodos() {
        return filmeDAO.listAll().stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public Filme buscarPorId(Long id) {
        Filme filme = filmeDAO.findById(id);
        if (filme == null) {
            throw new RuntimeException("Filme não encontrado com o ID fornecido.");
        }
        return filme;
    }

    @Transactional
    public FilmeResponseDTO cadastrar(Filme filme) {
        // Busca a referência gerenciada do Plano no banco de dados antes de salvar
        if (filme.getPlano() != null && filme.getPlano().getId() != null) {
            Plano planoGerenciado = filmeDAO.getEntityManager().getReference(Plano.class, filme.getPlano().getId());
            filme.setPlano(planoGerenciado);
        }

        filmeDAO.persist(filme);
        return converterParaDTO(filme);
    }

    @Transactional
    public FilmeResponseDTO atualizar(Long id, Filme filmeAtualizado) {
        Filme filme = buscarPorId(id);

        filme.setTitulo(filmeAtualizado.getTitulo());
        filme.setDescricao(filmeAtualizado.getDescricao());
        filme.setGenero(filmeAtualizado.getGenero());
        filme.setCaminhoImagemHorizontal(filmeAtualizado.getCaminhoImagemHorizontal());
        filme.setCaminhoImagemVertical(filmeAtualizado.getCaminhoImagemVertical());

        // Atualiza a referência do Plano
        if (filmeAtualizado.getPlano() != null && filmeAtualizado.getPlano().getId() != null) {
            Plano planoGerenciado = filmeDAO.getEntityManager().getReference(Plano.class, filmeAtualizado.getPlano().getId());
            filme.setPlano(planoGerenciado);
        }

        return converterParaDTO(filme);
    }

    @Transactional
    public void deletar(Long id) {
        Filme filme = buscarPorId(id);
        filmeDAO.delete(filme);
    }

    private FilmeResponseDTO converterParaDTO(Filme filme) {
        FilmeResponseDTO dto = new FilmeResponseDTO();

        dto.setId(filme.getId());
        dto.setTitulo(filme.getTitulo());
        dto.setDescricao(filme.getDescricao());
        dto.setGenero(filme.getGenero());
        dto.setCaminhoImagemHorizontal(filme.getCaminhoImagemHorizontal());
        dto.setCaminhoImagemVertical(filme.getCaminhoImagemVertical());

        if (filme.getPlano() != null) {
            dto.setPlanoId(filme.getPlano().getId());
        }

        return dto;
    }
}