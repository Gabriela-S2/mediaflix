package br.com.ifg.resource;

import br.com.ifg.dto.RedefinirSenhaRequestDTO;
import br.com.ifg.service.RecuperacaoSenhaService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/recuperacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecuperacaoSenhaResource {

    @Inject
    RecuperacaoSenhaService recuperacaoService;

    // Passo 1: Gera e envia o código para o e-mail
    @POST
    @Path("/gerar")
    @PermitAll
    public Response solicitarCodigo(EmailRequest request) {
        try {
            // O serviço gera o código e salva no banco
            recuperacaoService.gerarCodigoRecuperacao(request.email);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    // Passo 2: Valida o código e altera a senha
    @POST
    @Path("/redefinir")
    @PermitAll
    public Response redefinirSenha(@Valid RedefinirSenhaRequestDTO dto) {
        boolean sucesso = recuperacaoService.redefinirSenha(dto.email(), dto.codigo(), dto.novaSenha());

        if (sucesso) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Código inválido ou expirado\"}")
                    .build();
        }
    }

    // Classe auxiliar simples para receber apenas o e-mail no Passo 1
    public static class EmailRequest {
        public String email;
    }
}