package br.com.ifg.service;

import br.com.ifg.model.dao.RecuperacaoSenhaDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.entity.RecuperacaoSenha;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class RecuperacaoSenhaService {

    @Inject
    RecuperacaoSenhaDAO recuperacaoDAO;

    @Inject
    UsuarioDAO usuarioDAO;

    @Transactional
    public String gerarCodigoRecuperacao(String email) {
        String codigoGerado = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setEmail(email);
        recuperacao.setCodigo(codigoGerado);

        recuperacaoDAO.persist(recuperacao);

        return codigoGerado;
    }

    @Transactional
    public boolean redefinirSenha(String email, String codigoDigitado, String novaSenha) {
        RecuperacaoSenha recuperacao = recuperacaoDAO.buscarCodigoPorEmail(email);

        if (recuperacao != null && recuperacao.getCodigo().equals(codigoDigitado)) {
            // Implementação real do BCrypt exigida pela regra 17
            String senhaComHash = BcryptUtil.bcryptHash(novaSenha);

            usuarioDAO.atualizarSenha(email, senhaComHash);
            recuperacaoDAO.limparCodigosUsados(email);

            return true;
        }
        return false;
    }
}