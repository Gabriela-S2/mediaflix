package br.com.ifg.controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/cadastro")
public class CadastroController {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance cadastro();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance cadastro(){

        return CadastroController.Templates.cadastro();
    }
}
