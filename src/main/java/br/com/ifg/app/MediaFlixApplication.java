package br.com.ifg.app;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.hibernate.sql.Template;

import static java.util.Objects.requireNonNull;

// Define o prefixo "/api" para todos os seus Resources (Controllers)
@ApplicationPath("/")
public class MediaFlixApplication extends Application {
}
