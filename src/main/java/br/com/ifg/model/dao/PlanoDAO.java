// PlanoDAO.java
package br.com.ifg.model.dao;

import br.com.ifg.model.entity.Plano;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlanoDAO implements PanacheRepository<Plano> {

    public Plano buscarPorNome(String nome) {
        return find("SELECT p FROM Plano p WHERE p.nome = :nome",
                Parameters.with("nome", nome)).firstResult();
    }

    public long contarPlanos() {
        return count("SELECT COUNT(p) FROM Plano p");
    }
}