package br.com.ifg.controller;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
public class RootController {

    @GET
    @PermitAll
    public Response redirecionarRaiz() {
        return Response.status(Response.Status.FOUND)
                .header("Location", "/index")
                .build();
    }
}