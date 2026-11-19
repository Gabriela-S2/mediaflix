// RecuperacaoSenhaDAO.java
package br.com.ifg.model.dao;

import br.com.ifg.model.entity.RecuperacaoSenha;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecuperacaoSenhaDAO implements PanacheRepository<RecuperacaoSenha> {

    public RecuperacaoSenha buscarCodigoPorEmail(String email) {
        return find("SELECT r FROM RecuperacaoSenha r WHERE r.email = :email ORDER BY r.dataCriacao DESC",
                Parameters.with("email", email)).firstResult();
    }

    public void limparCodigosUsados(String email) {
        delete("DELETE FROM RecuperacaoSenha r WHERE r.email = :email",
                Parameters.with("email", email));
    }
}