package br.com.ifg.exception;

import br.com.ifg.model.dto.ErroResponseDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.List;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> detalhes = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        ErroResponseDTO erro = new ErroResponseDTO(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Dados de entrada inválidos.",
                detalhes,
                LocalDateTime.now()
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(erro)
                .build();
    }
}