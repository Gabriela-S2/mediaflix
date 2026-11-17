package br.com.ifg.controller;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/filmes")
public class AdminFilmesController {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance adminFilmes();
    }

    @GET
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance paginaAdmin() {
        return Templates.adminFilmes();
    }
}