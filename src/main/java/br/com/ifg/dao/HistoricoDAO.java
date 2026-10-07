package br.com.ifg.dao;

import br.com.ifg.model.Historico;
import br.com.ifg.model.HistoricoId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
// Note que usamos PanacheRepositoryBase porque a chave primária é composta (UsuarioFilmeId)
public class HistoricoDAO implements PanacheRepositoryBase<Historico, HistoricoId> {
}