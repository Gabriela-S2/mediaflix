package br.com.ifg.resources;

import br.com.ifg.model.dto.RedefinirSenhaRequestDTO;
import br.com.ifg.model.dto.SolicitarRecuperacaoRequestDTO;
import br.com.ifg.service.RecuperacaoSenhaService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/recuperacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecuperacaoSenhaResource {

    @Inject
    RecuperacaoSenhaService recuperacaoService;

    @POST
    @Path("/gerar")
    @PermitAll
    public Response solicitarCodigo(@Valid SolicitarRecuperacaoRequestDTO request) {
        recuperacaoService.gerarCodigoRecuperacao(request.email());
        return Response.ok(Map.of("mensagem", "Código de recuperação gerado com sucesso.")).build();
    }

    @POST
    @Path("/redefinir")
    @PermitAll
    public Response redefinirSenha(@Valid RedefinirSenhaRequestDTO dto) {
        boolean sucesso = recuperacaoService.redefinirSenha(dto.email(), dto.codigo(), dto.novaSenha());
        if (sucesso) {
            return Response.ok(Map.of("mensagem", "Senha redefinida com sucesso.")).build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("erro", "Código inválido ou expirado."))
                .build();
    }
}