package br.com.ifg.model.bo;

import br.com.ifg.model.entity.RecuperacaoSenha;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@ApplicationScoped
public class RecuperacaoSenhaBO {

    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TAMANHO_CODIGO = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String gerarCodigoSeguro() {
        StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            sb.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
        }
        return sb.toString();
    }

    public boolean validarCodigoRecuperacao(RecuperacaoSenha recuperacao, String codigoDigitado, int expiracaoMinutos) {
        if (recuperacao == null || codigoDigitado == null) {
            return false;
        }
        boolean dentroDoPrazo = recuperacao.getDataCriacao()
                .plusMinutes(expiracaoMinutos)
                .isAfter(LocalDateTime.now());

        return dentroDoPrazo && recuperacao.getCodigo().equalsIgnoreCase(codigoDigitado.trim());
    }
}