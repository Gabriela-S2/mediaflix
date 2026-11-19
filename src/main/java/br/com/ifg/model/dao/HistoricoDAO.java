package br.com.ifg.model.dao;

import br.com.ifg.model.entity.Historico;
import br.com.ifg.model.entity.HistoricoId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class HistoricoDAO implements PanacheRepositoryBase<Historico, HistoricoId> {

    public List<Historico> listarPorUsuario(Long usuarioId) {
        return find(
                "SELECT h FROM Historico h JOIN FETCH h.filme WHERE h.usuario.id = :usuarioId",
                Parameters.with("usuarioId", usuarioId)
        ).list();
    }
}