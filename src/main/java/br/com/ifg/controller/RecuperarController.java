package br.com.ifg.controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/esqueci-minha-senha")
public class RecuperarController {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance recuperarSenha();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance recuperarSenha(){

        return RecuperarController.Templates.recuperarSenha();
    }
}
