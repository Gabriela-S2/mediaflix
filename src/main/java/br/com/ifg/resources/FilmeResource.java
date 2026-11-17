package br.com.ifg.resources;

import br.com.ifg.model.dto.FilmeResponseDTO;
import br.com.ifg.model.entity.Filme;
import br.com.ifg.service.FilmeService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/filmes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FilmeResource {

    @Inject
    FilmeService filmeService;

    // ==========================================
    // ROTA PARA UTILIZADORES COMUNS
    // ==========================================
    @GET
    @Authenticated
    public Response listarFilmesPermitidos() {
        List<FilmeResponseDTO> filmes = filmeService.buscarFilmesPorPlanoDoUsuarioLogado();
        return Response.ok(filmes).build();
    }

    // ==========================================
    // ROTAS EXCLUSIVAS PARA ADMINISTRADORES
    // ==========================================

    @GET
    @Path("/todos")
    @RolesAllowed("ADMIN")
    public Response listarTodosAdmin() {
        // Ignora a restrição de plano e traz tudo para o painel de gestão
        List<FilmeResponseDTO> filmes = filmeService.listarTodos();
        return Response.ok(filmes).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response cadastrar(Filme filme) {
        try {
            FilmeResponseDTO novoFilme = filmeService.cadastrar(filme);
            return Response.status(Response.Status.CREATED).entity(novoFilme).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Erro ao criar: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response atualizar(@PathParam("id") Long id, Filme filme) {
        try {
            FilmeResponseDTO filmeAtualizado = filmeService.atualizar(id, filme);
            return Response.ok(filmeAtualizado).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Filme não encontrado.").build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deletar(@PathParam("id") Long id) {
        try {
            filmeService.deletar(id);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Filme não encontrado.").build();
        }
    }
}