package br.com.ifg.exception;

import br.com.ifg.model.dto.ErroResponseDTO;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webEx) {
            int status = webEx.getResponse().getStatus();
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErroResponseDTO(status, webEx.getMessage()))
                    .build();
        }

        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErroResponseDTO(400, exception.getMessage()))
                    .build();
        }

        if (exception instanceof SecurityException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErroResponseDTO(403, exception.getMessage()))
                    .build();
        }

        LOG.error("Erro interno não tratado no servidor", exception);

        ErroResponseDTO erro = new ErroResponseDTO(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Ocorreu um erro interno inesperado no servidor."
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(erro)
                .build();
    }
}