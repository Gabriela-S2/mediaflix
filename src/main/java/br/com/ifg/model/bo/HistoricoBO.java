package br.com.ifg.model.bo;

import br.com.ifg.model.entity.Filme;
import br.com.ifg.model.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HistoricoBO {

    @Inject
    FilmeBO filmeBO;

    public void validarRegistroProgresso(Usuario usuario, Filme filme, Long tempoAssistido) {
        if (filme == null) {
            throw new IllegalArgumentException("Filme não encontrado.");
        }
        if (tempoAssistido == null || tempoAssistido < 0) {
            throw new IllegalArgumentException("O tempo assistido não pode ser negativo.");
        }
        filmeBO.validarAcessoAoFilme(usuario, filme);
    }
}