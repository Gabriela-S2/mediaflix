package br.com.ifg.model.bo;

import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.NewCookie;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

public class AuthenticationBO {
    @ServerResponseFilter()
    public void responseFilter(ContainerResponseContext responseContext) {

        if (responseContext.getHeaderString("token") != null) {

            NewCookie cookie = new NewCookie.Builder("jwt-cookie")
                    .value("token")
                    .path("/")
                    .maxAge(3600)
                    .secure(false)
                    .httpOnly(true)
                    .build();

            responseContext.getHeaders().add("Set-Cookie", cookie);
        }
    }
}
