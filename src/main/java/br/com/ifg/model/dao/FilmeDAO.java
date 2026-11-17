package br.com.ifg.model.dao;

import br.com.ifg.model.entity.Filme;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;

import java.util.List;

@SessionScoped
public class FilmeDAO implements PanacheRepository<Filme> {

    // Busca todos os filmes disponíveis para um determinado nível de plano
    public List<Filme> buscarFilmesPorNivelPlano(Integer nivelUsuario) {
        // Ex: Se o usuário é nível 2 (Padrão), ele vê filmes de nível 1 e 2.
        return list("plano.nivel <= ?1", nivelUsuario);
    }
}