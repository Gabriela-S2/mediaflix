// UsuarioDAO.java
package br.com.ifg.model.dao;

import br.com.ifg.model.entity.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UsuarioDAO implements PanacheRepository<Usuario> {

    public Usuario buscarPorEmail(String email) {
        return find("SELECT u FROM Usuario u WHERE u.email = :email",
                Parameters.with("email", email)).firstResult();
    }

    public Usuario buscarPorUsername(String username) {
        return find("SELECT u FROM Usuario u WHERE u.username = :username",
                Parameters.with("username", username)).firstResult();
    }

    public void atualizarSenha(String email, String novaSenha) {
        update("UPDATE Usuario u SET u.senha = :senha WHERE u.email = :email",
                Parameters.with("senha", novaSenha).and("email", email));
    }
}