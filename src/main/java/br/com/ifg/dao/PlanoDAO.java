package br.com.ifg.dao;

import br.com.ifg.model.Plano;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlanoDAO implements PanacheRepository<Plano> {
    public Plano buscarPorNome(String nome) {
        return find("nome", nome).firstResult();
    }
}