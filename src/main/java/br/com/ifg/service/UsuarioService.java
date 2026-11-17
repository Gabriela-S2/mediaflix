package br.com.ifg.service;

import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.entity.Usuario;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;

@ApplicationScoped
public class UsuarioService {

    @Inject
    UsuarioDAO usuarioDAO;


    @Transactional
    public Usuario cadastrar(Usuario usuario) {
        // É obrigatório encriptar antes de guardar
        String senhaCriptografada = BcryptUtil.bcryptHash(usuario.getSenha());
        usuario.setSenha(senhaCriptografada);
        usuarioDAO.persist(usuario);
        return usuario;
    }

    public String autenticar(String email, String senha) {
        Usuario usuario = usuarioDAO.find("email", email).firstResult();
        if (usuario == null) {
            throw new RuntimeException("Utilizador não encontrado.");
        }

        // Valida comparando a palavra-passe em texto limpo com o hash BCrypt guardado
        if (!BcryptUtil.matches(senha, usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta.");
        }

        // Geração do token JWT válido por 24 horas
        return Jwt.issuer("https://mediaflix.ifg.edu.br")
                .upn(usuario.getEmail())
                .groups(Boolean.TRUE.equals(usuario.getAdmin()) ? "ADMIN" : "USER")
                .expiresIn(Duration.ofHours(24))
                .sign();
    }
}