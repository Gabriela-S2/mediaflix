package br.com.ifg.service;

import br.com.ifg.dao.FilmeDAO;
import br.com.ifg.dao.UsuarioDAO;
import br.com.ifg.dto.FilmeResponseDTO;
import br.com.ifg.model.Filme;
import br.com.ifg.model.Usuario;
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

        // Exemplo de filtro baseado no plano:
        // Adapte a query caso a classe Plano tenha um atributo "nivel" ou "id" de hierarquia.
        List<Filme> filmesDb = filmeDAO.listAll();

        return filmesDb.stream()
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
        filmeDAO.persist(filme);
        return converterParaDTO(filme);
    }

    @Transactional
    public FilmeResponseDTO atualizar(Long id, Filme filmeAtualizado) {
        Filme filme = buscarPorId(id);

        // Setters correspondentes aos atributos reais da sua classe Filme.java
        filme.setTitulo(filmeAtualizado.getTitulo());
        filme.setDescricao(filmeAtualizado.getDescricao());
        filme.setGenero(filmeAtualizado.getGenero());
        filme.setCaminhoImagemHorizontal(filmeAtualizado.getCaminhoImagemHorizontal());
        filme.setCaminhoImagemVertical(filmeAtualizado.getCaminhoImagemVertical());
        filme.setPlano(filmeAtualizado.getPlano());

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

        // Evite mapear a entidade Plano inteira no DTO para não vazar a estrutura do banco.
        // Mapeie apenas o nome do plano ou o ID, caso necessário.
        if (filme.getPlano() != null) {
            dto.setPlanoId(filme.getPlano().getId());
        }

        return dto;
    }
}