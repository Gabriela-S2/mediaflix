// RecuperacaoSenhaService.java
package br.com.ifg.service;

import br.com.ifg.model.dao.RecuperacaoSenhaDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.entity.RecuperacaoSenha;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class RecuperacaoSenhaService {

    private static final int EXPIRACAO_MINUTOS = 15;

    @Inject
    RecuperacaoSenhaDAO recuperacaoDAO;

    @Inject
    UsuarioDAO usuarioDAO;

    @Transactional
    public String gerarCodigoRecuperacao(String email) {
        if (usuarioDAO.buscarPorEmail(email) == null) {
            throw new IllegalArgumentException("E-mail não encontrado.");
        }

        recuperacaoDAO.limparCodigosUsados(email);
        String codigoGerado = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setEmail(email);
        recuperacao.setCodigo(codigoGerado);
        recuperacao.setDataCriacao(LocalDateTime.now());

        recuperacaoDAO.persist(recuperacao);
        return codigoGerado;
    }

    @Transactional
    public boolean redefinirSenha(String email, String codigoDigitado, String novaSenha) {
        RecuperacaoSenha recuperacao = recuperacaoDAO.buscarCodigoPorEmail(email);
        if (recuperacao == null) {
            return false;
        }

        boolean naoExpirado = recuperacao.getDataCriacao()
                .plusMinutes(EXPIRACAO_MINUTOS)
                .isAfter(LocalDateTime.now());

        if (naoExpirado && recuperacao.getCodigo().equalsIgnoreCase(codigoDigitado.trim())) {
            String senhaComHash = BcryptUtil.bcryptHash(novaSenha);
            usuarioDAO.atualizarSenha(email, senhaComHash);
            recuperacaoDAO.limparCodigosUsados(email);
            return true;
        }
        return false;
    }
}