package br.com.ifg.resources;

import jakarta.ws.rs.core.NewCookie;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.AtualizarPerfilRequestDTO;
import br.com.ifg.model.dto.LoginRequestDTO;
import br.com.ifg.model.dto.UsuarioRequestDTO;
import br.com.ifg.model.dto.UsuarioResponseDTO;
import br.com.ifg.model.entity.Usuario;
import br.com.ifg.service.UsuarioService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.util.Map;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    UsuarioService usuarioService;

    @Inject
    UsuarioDAO usuarioDAO;

    @Inject
    JsonWebToken jwt;

    @POST
    @PermitAll
    public Response cadastrar(@Valid UsuarioRequestDTO dto) {
        UsuarioResponseDTO novoUsuario = usuarioService.cadastrar(dto);
        return Response.status(Response.Status.CREATED).entity(novoUsuario).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequestDTO credenciais) {
        try {
            String token = usuarioService.autenticar(credenciais.email(), credenciais.senha());
            Usuario usuario = usuarioDAO.buscarPorEmail(credenciais.email());
            boolean isAdmin = Boolean.TRUE.equals(usuario.getIsAdmin());

            NewCookie jwtCookie = new NewCookie.Builder("jwt-token")
                    .value(token)
                    .path("/")
                    .maxAge(86400) // 24 horas (em segundos)
                    .httpOnly(true)
                    .sameSite(NewCookie.SameSite.LAX)
                    .build();

            return Response.ok(Map.of(
                    "mensagem", "Login com sucesso",
                    "isAdmin", isAdmin
            )).cookie(jwtCookie).build();

        } catch (Exception e) {
            String mensagemErro = e.getMessage() != null ? e.getMessage() : "Credenciais inválidas.";
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("erro", mensagemErro)).build();
        }
    }

    @POST
    @Path("/logout")
    @PermitAll
    public Response logout() {
        NewCookie cookieExpirado = new NewCookie.Builder("jwt-token")
                .value("")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.LAX)
                .build();

        return Response.ok(Map.of("mensagem", "Logout realizado com sucesso"))
                .cookie(cookieExpirado)
                .build();
    }

    @GET
    @Path("/perfil")
    @Authenticated
    public Response obterPerfil() {
        UsuarioResponseDTO perfil = usuarioService.buscarPerfil(jwt.getName());
        return Response.ok(perfil).build();
    }

    @PUT
    @Path("/perfil")
    @Authenticated
    public Response atualizarPerfil(@Valid AtualizarPerfilRequestDTO dto) {
        UsuarioResponseDTO perfilAtualizado = usuarioService.atualizarPerfil(jwt.getName(), dto);
        return Response.ok(perfilAtualizado).build();
    }

    @POST
    @Path("/perfil/foto")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFotoPerfil(@RestForm("foto") FileUpload foto) {
        UsuarioResponseDTO perfilAtualizado = usuarioService.salvarFotoPerfil(jwt.getName(), foto);
        return Response.ok(perfilAtualizado).build();
    }

    @GET
    @Path("/foto/{nomeArquivo}")
    @PermitAll
    @Produces("image/png")
    public Response buscarFotoPerfil(@PathParam("nomeArquivo") String nomeArquivo) {
        try {
            java.nio.file.Path caminhoFoto = usuarioService.obterCaminhoFoto(nomeArquivo);
            return Response.ok(caminhoFoto.toFile())
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/privacidade")
    @io.quarkus.security.Authenticated
    public jakarta.ws.rs.core.Response consultarPrivacidade(@jakarta.ws.rs.core.Context jakarta.ws.rs.core.SecurityContext ctx) {
        boolean ativa = usuarioService.consultarPreferenciaTelemetria(ctx.getUserPrincipal().getName());
        return jakarta.ws.rs.core.Response.ok(java.util.Map.of("telemetriaAtiva", ativa)).build();
    }

    @PUT
    @Path("/privacidade")
    @io.quarkus.security.Authenticated
    public jakarta.ws.rs.core.Response atualizarPrivacidade(
            @jakarta.ws.rs.core.Context jakarta.ws.rs.core.SecurityContext ctx,
            java.util.Map<String, Boolean> body
    ) {
        boolean ativa = Boolean.TRUE.equals(body.get("telemetriaAtiva"));
        boolean resultado = usuarioService.atualizarPreferenciaTelemetria(ctx.getUserPrincipal().getName(), ativa);
        return jakarta.ws.rs.core.Response.ok(java.util.Map.of("telemetriaAtiva", resultado)).build();
    }
}