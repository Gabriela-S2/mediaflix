package br.com.ifg.app;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

// Define o prefixo "/api" para todos os seus Resources (Controllers)
@ApplicationPath("/api")
public class MediaFlixApplication extends Application {
}