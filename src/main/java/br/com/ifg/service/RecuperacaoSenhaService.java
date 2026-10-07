package br.com.ifg.service;

import br.com.ifg.dao.RecuperacaoSenhaDAO;
import br.com.ifg.dao.UsuarioDAO;
import br.com.ifg.model.RecuperacaoSenha;
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

    // REGRA 15: Anotação @Transactional obrigatória nos métodos de negócio que alteram o banco
    @Transactional
    public String gerarCodigoRecuperacao(String email) {
        // 1. Lógica de negócio: Validar se o usuário existe (omitido para brevidade)

        // 2. Lógica de negócio: Gerar um código aleatório de 6 caracteres
        String codigoGerado = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 3. Montar a entidade
        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setEmail(email);
        recuperacao.setCodigo(codigoGerado);

        // 4. Chamar o DAO para o trabalho braçal de salvar
        recuperacaoDAO.persist(recuperacao);

        return codigoGerado; // Esse código seria enviado por e-mail na vida real
    }

    @Transactional
    public boolean redefinirSenha(String email, String codigoDigitado, String novaSenha) {
        // 1. Usa o DAO para buscar o código no banco
        RecuperacaoSenha recuperacao = recuperacaoDAO.buscarCodigoPorEmail(email);

        // 2. Lógica de negócio: Verifica se o código bate
        if (recuperacao != null && recuperacao.getCodigo().equals(codigoDigitado)) {

            // Lógica de segurança (Regra 17): Aqui você faria o Hash da novaSenha antes de salvar!
            String senhaComHash = aplicarHashBcrypt(novaSenha);

            // 3. Usa os DAOs para atualizar a senha e limpar os códigos antigos
            usuarioDAO.atualizarSenha(email, senhaComHash);
            recuperacaoDAO.limparCodigosUsados(email);

            return true;
        }
        return false;
    }

    private String aplicarHashBcrypt(String senha) {
        // Implementação do Bcrypt exigida pela regra 17
        return senha; // Apenas ilustrativo
    }
}