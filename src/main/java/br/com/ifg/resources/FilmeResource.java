package br.com.ifg.resources;

import br.com.ifg.dto.FilmeResponseDTO;
import br.com.ifg.service.FilmeService;
import io.quarkus.security.Authenticated;
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

    @GET
    @Authenticated
    public Response listarFilmesPermitidos() {
        List<FilmeResponseDTO> filmes = filmeService.buscarFilmesPorPlanoDoUsuarioLogado();
        return Response.ok(filmes).build();
    }
}