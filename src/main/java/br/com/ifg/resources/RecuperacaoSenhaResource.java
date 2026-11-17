package br.com.ifg.resources;

import br.com.ifg.model.dto.RedefinirSenhaRequestDTO;
import br.com.ifg.service.RecuperacaoSenhaService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;

@Path("/recuperacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecuperacaoSenhaResource {

    @Inject
    RecuperacaoSenhaService recuperacaoService;

    @POST
    @Path("/gerar")
    @PermitAll
    public Response solicitarCodigo(EmailRequest request) {
        try {
            recuperacaoService.gerarCodigoRecuperacao(request.getEmail());
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/redefinir")
    @PermitAll
    public Response redefinirSenha(@Valid RedefinirSenhaRequestDTO dto) {
        // Se RedefinirSenhaRequestDTO for um "record", dto.email() está correto.
        // Se for uma classe normal, mude para dto.getEmail().
        boolean sucesso = recuperacaoService.redefinirSenha(dto.email(), dto.codigo(), dto.novaSenha());

        if (sucesso) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Código inválido ou expirado\"}")
                    .build();
        }
    }

    // Encapsulamento adicionado para evitar falhas na desserialização do JSON
    @Setter
    @Getter
    public static class EmailRequest {
        private String email;

    }
}