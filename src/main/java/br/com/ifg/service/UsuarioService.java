package br.com.ifg.service;

import br.com.ifg.dao.UsuarioDAO;
import br.com.ifg.model.Usuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@ApplicationScoped
public class UsuarioService {

    @Inject
    UsuarioDAO usuarioDAO;

    @Transactional
    public Usuario cadastrar(Usuario usuario) {
        // Criptografia nativa usando SHA-256
        String senhaCriptografada = hashSenha(usuario.getSenha());
        usuario.setSenha(senhaCriptografada);
        usuarioDAO.persist(usuario);
        return usuario;
    }

    public String autenticar(String email, String senha) {
        Usuario usuario = usuarioDAO.find("email", email).firstResult();
        if (usuario == null) {
            throw new RuntimeException("Utilizador não encontrado.");
        }

        // Valida comparando o hash SHA-256 da senha digitada com a salva no banco
        String senhaDigitadaHash = hashSenha(senha);
        if (!senhaDigitadaHash.equals(usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta.");
        }

        // Geração do token JWT válido por 24 horas
        // CORREÇÃO: Utilização do getAdmin() em vez do inexistente getTipo()
        return Jwt.issuer("https://mediaflix.ifg.edu.br")
                .upn(usuario.getEmail())
                .groups(Boolean.TRUE.equals(usuario.getAdmin()) ? "ADMIN" : "USER")
                .expiresIn(Duration.ofHours(24))
                .sign();
    }

    // Método auxiliar privado para gerar o hash SHA-256 nativamente
    private String hashSenha(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(senha.getBytes(StandardCharsets.UTF_8));

            // CORREÇÃO: Adicionado o operador de multiplicação '*'
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);

            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao criptografar senha", e);
        }
    }
}