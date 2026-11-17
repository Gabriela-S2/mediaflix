package br.com.ifg.resources;

import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.LoginRequestDTO;
import br.com.ifg.model.entity.Usuario;
import br.com.ifg.service.UsuarioService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import jakarta.ws.rs.core.NewCookie;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    UsuarioService usuarioService;

    @Inject
    UsuarioDAO usuarioDAO;

    @POST
    @PermitAll
    public Response cadastrar(Usuario usuario) {
        try {
            Usuario novoUsuario = usuarioService.cadastrar(usuario);
            return Response.status(Response.Status.CREATED).entity(novoUsuario).build();
        } catch (Exception e) {
            String mensagemErro = e.getMessage() != null ? e.getMessage() : "Erro desconhecido no cadastro.";
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("erro", mensagemErro)).build();
        }
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequestDTO credenciais) {
        try {
            String token = usuarioService.autenticar(credenciais.email(), credenciais.senha());

            // Busca o utilizador para verificar se é admin
            Usuario usuario = usuarioDAO.buscarPorEmail(credenciais.email());
            boolean isAdmin = usuario.getAdmin() != null && usuario.getAdmin();

            NewCookie jwtCookie = new NewCookie.Builder("jwt-token")
                    .value(token)
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .httpOnly(true)
                    .build();

            // Retorna o status de admin no JSON para o frontend fazer o redirecionamento
            return Response.ok(Map.of(
                            "mensagem", "Login com sucesso",
                            "isAdmin", isAdmin
                    ))
                    .cookie(jwtCookie)
                    .build();

        } catch (Exception e) {
            String mensagemErro = e.getMessage() != null ? e.getMessage() : "Erro interno do servidor.";
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("erro", mensagemErro)).build();
        }
    }
}