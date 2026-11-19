package br.com.ifg.model.dao;

import br.com.ifg.model.entity.Filme;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FilmeDAO implements PanacheRepository<Filme> {

    public List<Filme> buscarFilmesPorNivelPlano(Integer nivelUsuario) {
        return find(
                "SELECT f FROM Filme f WHERE f.plano IS NULL OR f.plano.nivel <= :nivel ORDER BY f.id ASC",
                Parameters.with("nivel", nivelUsuario)
        ).list();
    }

    public List<Filme> listarTodosOrdenados() {
        return find("SELECT f FROM Filme f ORDER BY f.id ASC").list();
    }

    public void removerVinculosDoFilme(Long filmeId) {
        getEntityManager().createNativeQuery("DELETE FROM usuario_lista WHERE filme_id = :id")
                .setParameter("id", filmeId)
                .executeUpdate();
        getEntityManager().createNativeQuery("DELETE FROM usuario_curtidas WHERE filme_id = :id")
                .setParameter("id", filmeId)
                .executeUpdate();
        getEntityManager().createNativeQuery("DELETE FROM usuario_filmes WHERE filme_id = :id")
                .setParameter("id", filmeId)
                .executeUpdate();
    }
}