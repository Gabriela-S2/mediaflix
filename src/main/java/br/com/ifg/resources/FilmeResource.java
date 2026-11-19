// FilmeResource.java
package br.com.ifg.resources;

import br.com.ifg.model.dto.FilmeRequestDTO;
import br.com.ifg.model.dto.FilmeResponseDTO;
import br.com.ifg.service.FilmeService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/filmes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FilmeResource {

    @Inject
    FilmeService filmeService;

    @GET
    @Authenticated
    public Response listarFilmesPermitidos() {
        List<FilmeResponseDTO> filmes = filmeService.buscarFilmesPorPlanoDoUsuarioLogado();
        return Response.ok(filmes).build();
    }

    @GET
    @Path("/{id}")
    @Authenticated
    public Response buscarPorId(@PathParam("id") Long id) {
        try {
            FilmeResponseDTO filme = filmeService.buscarPorIdParaUsuarioLogado(id);
            return Response.ok(filme).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("erro", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("erro", e.getMessage())).build();
        }
    }

    @GET
    @Path("/todos")
    @RolesAllowed("ADMIN")
    public Response listarTodosAdmin() {
        List<FilmeResponseDTO> filmes = filmeService.listarTodos();
        return Response.ok(filmes).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response cadastrar(@Valid FilmeRequestDTO dto) {
        FilmeResponseDTO novoFilme = filmeService.cadastrar(dto);
        return Response.status(Response.Status.CREATED).entity(novoFilme).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response atualizar(@PathParam("id") Long id, @Valid FilmeRequestDTO dto) {
        FilmeResponseDTO filmeAtualizado = filmeService.atualizar(id, dto);
        return Response.ok(filmeAtualizado).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deletar(@PathParam("id") Long id) {
        filmeService.deletar(id);
        return Response.noContent().build();
    }
}