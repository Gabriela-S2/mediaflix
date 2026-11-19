package br.com.ifg.resources;

import br.com.ifg.model.dto.HistoricoRequestDTO;
import br.com.ifg.model.dto.HistoricoResponseDTO;
import br.com.ifg.service.HistoricoService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/historico")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class HistoricoResource {

    @Inject
    HistoricoService historicoService;

    @GET
    public Response listarHistorico() {
        List<HistoricoResponseDTO> lista = historicoService.listarMeuHistorico();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/{filmeId}")
    public Response buscarProgresso(@PathParam("filmeId") Long filmeId) {
        return Response.ok(historicoService.buscarPorFilme(filmeId)).build();
    }

    @POST
    public Response salvarProgresso(@Valid HistoricoRequestDTO dto) {
        HistoricoResponseDTO atualizado = historicoService.registrarProgresso(dto);
        return Response.ok(atualizado).build();
    }
}