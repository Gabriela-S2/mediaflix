package br.com.ifg.controller;

import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.time.ZonedDateTime;

public class JWTController {
    @Path("generate")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response generateToken(){
        String token = Jwt
                .issuer("http://localhost/issuer")
                .upn("gabriela@ifg.edu.br")
                .groups("Admin")
                .claim("nome", "Gabriela Ribeiro")
                .claim("data", ZonedDateTime.now().toString())
                .sign();
        NewCookie cookie = new NewCookie.Builder("jwt-cookie")
                .value(token)
                .path("/")
                .maxAge(3600)
                .secure(false)
                .httpOnly(true)
                .build();
        return Response.ok(token).cookie(cookie).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/permitido")
    @PermitAll
    public Response permitido(){
        return Response.ok("permitido").build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/restrito")
    @RolesAllowed({"Admin"})
    public Response restrito(){
        return Response.ok("restrito").build();
    }

}
