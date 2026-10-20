package br.com.ifg.resources;

import br.com.ifg.model.Usuario;
import br.com.ifg.service.UsuarioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    UsuarioService usuarioService;

    @POST
    @Path("/cadastro")
    public Response cadastrar(Usuario usuario) {
        try {
            Usuario novoUsuario = usuarioService.cadastrar(usuario);
            return Response.status(Response.Status.CREATED).entity(novoUsuario).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("erro", e.getMessage())).build();
        }
    }

    @POST
    @Path("/login")
    public Response login(Usuario credenciais) {
        try {
            String token = usuarioService.autenticar(credenciais.getEmail(), credenciais.getSenha());
            return Response.ok(Map.of("token", token)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("erro", e.getMessage())).build();
        }
    }
}