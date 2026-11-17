package br.com.ifg.util;

import io.smallrye.jwt.build.Jwt;

import java.time.ZonedDateTime;

public class JWTUtil {
    public static String generateToken(String usuario, String email, String perfil){
        return Jwt
                .issuer("http://localhost/issuer")
                .upn(email)
                .groups(perfil)
                .claim("nome", "Gabriela Ribeiro")
                .claim("data", ZonedDateTime.now().toString())
                .sign();
    }
}
