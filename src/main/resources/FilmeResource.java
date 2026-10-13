package br.com.ifg.resources;

import br.com.ifg.dto.FilmeResponseDTO;
import br.com.ifg.service.FilmeService;
import jakarta.annotation.security.Authenticated;
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
    @Authenticated // Exige JWT no cabeçalho Authorization: Bearer <token>
    public Response listarFilmesPermitidos() {
        // A lógica de extrair o e-mail/id do token e filtrar os filmes 
        // pelo nível do plano ficará dentro do FilmeService.
        List<FilmeResponseDTO> filmes = filmeService.buscarFilmesPorPlanoDoUsuarioLogado();
        return Response.ok(filmes).build();
    }
}