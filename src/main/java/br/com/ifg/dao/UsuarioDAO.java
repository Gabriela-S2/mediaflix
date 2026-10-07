package br.com.ifg.dao;

import br.com.ifg.model.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UsuarioDAO implements PanacheRepository<Usuario> {

    // Método para buscar usuário pelo email (usado no Login e na Recuperação)
    public Usuario buscarPorEmail(String email) {
        return find("email", email).firstResult();
    }

    // Método para atualizar a senha após confirmar o código
    public void atualizarSenha(String email, String novaSenha) {
        update("senha = ?1 where email = ?2", novaSenha, email);
    }
}