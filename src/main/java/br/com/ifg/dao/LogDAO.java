package br.com.ifg.dao;

import br.com.ifg.model.LogUso;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LogDAO implements PanacheRepository<LogUso> {

    // Registo de log isolado numa transação para não falhar caso a requisição principal falhe
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void salvarLog(LogUso log) {
        persist(log);
    }
}