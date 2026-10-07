package br.com.ifg.dao;

import br.com.ifg.model.RecuperacaoSenha;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecuperacaoSenhaDAO implements PanacheRepository<RecuperacaoSenha> {

    // Busca o código de recuperação mais recente gerado para o e-mail
    public RecuperacaoSenha buscarCodigoPorEmail(String email) {
        return find("email = ?1 ORDER BY dataCriacao DESC", email).firstResult();
    }

    // Deleta os códigos de um e-mail após a senha ser redefinida com sucesso
    public void limparCodigosUsados(String email) {
        delete("email", email);
    }
}