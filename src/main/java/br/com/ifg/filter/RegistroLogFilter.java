package br.com.ifg.filter;

import br.com.ifg.model.LogUso;
import br.com.ifg.service.LogArquivoService;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Provider
public class RegistroLogFilter implements ContainerRequestFilter {

    @Inject
    LogArquivoService logArquivoService; // <--- Injetamos o novo serviço de ficheiros

    @Context
    HttpServerRequest request;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        LogUso log = new LogUso();

        if (resourceInfo.getResourceMethod() != null) {
            log.setAcaoExecutada(resourceInfo.getResourceMethod().getName());
        }

        log.setMetodoHttp(context.getMethod());
        log.setUrl(context.getUriInfo().getRequestUri().toString());
        log.setIpCliente(request.remoteAddress().host());

        // Define o utilizador ou "Anonimo" para requisições não autenticadas (como o Login)
        if (jwt != null && jwt.getName() != null) {
            log.setUsuarioExecutor(jwt.getName());
        } else {
            log.setUsuarioExecutor("Anonimo");
        }

        log.setHeaders(mascararHeaders(context.getHeaders()));
        log.setPayload(extrairEMascararPayload(context));

        // Guarda o registo no ficheiro TXT
        logArquivoService.salvarLogEmArquivo(log);
    }

    private String mascararHeaders(MultivaluedMap<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, values) -> {
            if (key.equalsIgnoreCase("Authorization") || key.toLowerCase().contains("token")) {
                sb.append("  ").append(key).append(": [MASKED]\n");
            } else {
                sb.append("  ").append(key).append(": ").append(values).append("\n");
            }
        });
        return sb.toString();
    }

    private String extrairEMascararPayload(ContainerRequestContext context) throws IOException {
        if (!context.hasEntity() || context.getMethod().equals("GET")) {
            return "  Sem Payload";
        }

        byte[] entityBytes = context.getEntityStream().readAllBytes();
        String payload = new String(entityBytes, StandardCharsets.UTF_8);

        context.setEntityStream(new ByteArrayInputStream(entityBytes));

        return "  " + payload.replaceAll("(\"senha\"\\s*:\\s*\")[^\"]+(\")", "$1[MASKED]$2")
                .replaceAll("(\"novaSenha\"\\s*:\\s*\")[^\"]+(\")", "$1[MASKED]$2");
    }
}