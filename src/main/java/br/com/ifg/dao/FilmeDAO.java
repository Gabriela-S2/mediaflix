package br.com.ifg.dao;

import br.com.ifg.model.Filme;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class FilmeDAO implements PanacheRepository<Filme> {

    // Busca todos os filmes disponíveis para um determinado nível de plano
    public List<Filme> buscarFilmesPorNivelPlano(Integer nivelUsuario) {
        // Ex: Se o usuário é nível 2 (Padrão), ele vê filmes de nível 1 e 2.
        return list("plano.nivel <= ?1", nivelUsuario);
    }
}