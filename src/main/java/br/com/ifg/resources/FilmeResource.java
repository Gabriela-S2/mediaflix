package br.com.ifg.resources;

import br.com.ifg.model.dto.CatalogoHomeResponseDTO;
import br.com.ifg.model.dto.FilmeRequestDTO;
import br.com.ifg.model.dto.FilmeResponseDTO;
import br.com.ifg.model.dto.InteracaoFilmeResponseDTO;
import br.com.ifg.service.FilmeService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.nio.file.Files;
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
    @Path("/catalogo")
    @Authenticated
    public Response obterCatalogoOrganizado() {
        CatalogoHomeResponseDTO catalogo = filmeService.montarCatalogoHome();
        return Response.ok(catalogo).build();
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
    @Path("/{id}/interacao")
    @Authenticated
    public Response consultarInteracao(@PathParam("id") Long id) {
        InteracaoFilmeResponseDTO status = filmeService.consultarStatusInteracao(id);
        return Response.ok(status).build();
    }

    @POST
    @Path("/{id}/lista")
    @Authenticated
    public Response alternarMinhaLista(@PathParam("id") Long id) {
        InteracaoFilmeResponseDTO status = filmeService.alternarMinhaLista(id);
        return Response.ok(status).build();
    }

    @POST
    @Path("/{id}/like")
    @Authenticated
    public Response alternarLike(@PathParam("id") Long id) {
        InteracaoFilmeResponseDTO status = filmeService.alternarCurtida(id);
        return Response.ok(status).build();
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

    @POST
    @Path("/{id}/imagens")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImagens(
            @PathParam("id") Long id,
            @RestForm("imagemHorizontal") FileUpload imagemHorizontal,
            @RestForm("imagemVertical") FileUpload imagemVertical
    ) {
        FilmeResponseDTO atualizado = filmeService.salvarImagensFilme(id, imagemHorizontal, imagemVertical);
        return Response.ok(atualizado).build();
    }

    @GET
    @Path("/imagem/{tipo}/{nomeArquivo}")
    @PermitAll
    public Response buscarImagemFilme(
            @PathParam("tipo") String tipo,
            @PathParam("nomeArquivo") String nomeArquivo
    ) {
        try {
            java.nio.file.Path caminho = filmeService.obterCaminhoImagem(tipo, nomeArquivo);
            String contentType = Files.probeContentType(caminho);
            if (contentType == null) {
                contentType = nomeArquivo.endsWith(".webp") ? "image/webp" : "image/png";
            }
            return Response.ok(caminho.toFile(), contentType)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deletar(@PathParam("id") Long id) {
        filmeService.deletar(id);
        return Response.noContent().build();
    }
}