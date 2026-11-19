package br.com.ifg.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class TcpAnalyticsClient {

    private static final Logger LOG = Logger.getLogger(TcpAnalyticsClient.class);
    private static final long TTL_CACHE_SEGUNDOS = 60;

    @ConfigProperty(name = "mediaflix.tcp.analytics.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "mediaflix.tcp.gist.id")
    Optional<String> gistId;

    @ConfigProperty(name = "mediaflix.tcp.gist.token")
    Optional<String> githubToken;

    @ConfigProperty(name = "mediaflix.tcp.lgpd.salt", defaultValue = "mediaflix-salt-local-2026")
    String lgpdSalt;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private volatile InetSocketAddress enderecoEmCache;
    private volatile Instant ultimaConsultaGist = Instant.EPOCH;

    /**
     * Registra eventos respeitando a minimização de dados (LGPD):
     * - VIEW: totalmente anônimo (sem identificador do usuário).
     * - LIKE / UNLIKE: envia apenas um hash SHA-256 irreversível para evitar votos duplicados.
     */
    public void registrarEventoAsync(String tipoEvento, Long filmeId, String tituloFilme, String emailUsuario) {
        if (!enabled || gistId.isEmpty() || gistId.get().isBlank()) {
            return;
        }

        String tokenAnonimizado = "VIEW".equalsIgnoreCase(tipoEvento)
                ? "anonimo"
                : gerarTokenPseudonimizado(emailUsuario);

        String mensagemJson = String.format(
                "{\"comando\":\"EVENTO\",\"tipo\":\"%s\",\"filmeId\":%d,\"titulo\":\"%s\",\"tokenUsuario\":\"%s\",\"timestamp\":\"%s\"}",
                tipoEvento,
                filmeId,
                tituloFilme != null ? tituloFilme.replace("\"", "'") : "",
                tokenAnonimizado,
                LocalDateTime.now()
        );

        CompletableFuture.runAsync(() -> {
            if (!enviarParaTcp(mensagemJson, false)) {
                enviarParaTcp(mensagemJson, true);
            }
        });
    }

    public Map<Long, Double> consultarRankingGlobal() {
        if (!enabled || gistId.isEmpty() || gistId.get().isBlank()) {
            return Map.of();
        }

        Map<Long, Double> resultado = consultarTcp(false);
        if (resultado == null) {
            resultado = consultarTcp(true);
        }
        return resultado != null ? resultado : Map.of();
    }

    private String gerarTokenPseudonimizado(String emailUsuario) {
        if (emailUsuario == null || emailUsuario.isBlank()) {
            return "anonimo";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String entrada = lgpdSalt + ":" + emailUsuario.trim().toLowerCase();
            byte[] hash = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));
            return "usr_" + HexFormat.of().formatHex(hash).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM.", e);
        }
    }

    private boolean enviarParaTcp(String payload, boolean forcarAtualizacaoGist) {
        InetSocketAddress destino = resolverEnderecoPinggy(forcarAtualizacaoGist);
        if (destino == null) return false;

        try (Socket socket = new Socket()) {
            socket.connect(destino, 1500);
            socket.setSoTimeout(2000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            out.println(payload);
            return true;
        } catch (Exception e) {
            LOG.debugf("Falha ao enviar evento TCP para %s (forceRefresh=%s): %s",
                    destino, forcarAtualizacaoGist, e.getMessage());
            enderecoEmCache = null;
            return false;
        }
    }

    private Map<Long, Double> consultarTcp(boolean forcarAtualizacaoGist) {
        InetSocketAddress destino = resolverEnderecoPinggy(forcarAtualizacaoGist);
        if (destino == null) return Map.of();

        Map<Long, Double> scores = new HashMap<>();
        try (Socket socket = new Socket()) {
            socket.connect(destino, 1200);
            socket.setSoTimeout(1500);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            out.println("{\"comando\":\"RANKING_GLOBAL\"}");
            String resposta = in.readLine();

            if (resposta != null && !resposta.isBlank()) {
                for (String par : resposta.split(",")) {
                    String[] partes = par.split(":");
                    if (partes.length == 2) {
                        scores.put(Long.parseLong(partes[0].trim()), Double.parseDouble(partes[1].trim()));
                    }
                }
            }
            return scores;
        } catch (Exception e) {
            LOG.debugf("Falha ao consultar ranking TCP em %s (forceRefresh=%s): %s",
                    destino, forcarAtualizacaoGist, e.getMessage());
            enderecoEmCache = null;
            return null;
        }
    }

    private synchronized InetSocketAddress resolverEnderecoPinggy(boolean forcarAtualizacao) {
        boolean cacheExpirado = Instant.now().isAfter(ultimaConsultaGist.plusSeconds(TTL_CACHE_SEGUNDOS));
        if (!forcarAtualizacao && enderecoEmCache != null && !cacheExpirado) {
            return enderecoEmCache;
        }

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/gists/" + gistId.get().trim()))
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .GET();

            if (githubToken.isPresent() && !githubToken.get().isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + githubToken.get().trim());
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warnf("Não foi possível ler o Gist %s (HTTP %d)", gistId.get(), response.statusCode());
                return enderecoEmCache;
            }

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject gistJson = reader.readObject();
                String conteudoArquivo = gistJson.getJsonObject("files")
                        .getJsonObject("mediaflix-tunnel.json")
                        .getString("content");

                try (JsonReader innerReader = Json.createReader(new StringReader(conteudoArquivo))) {
                    JsonObject tunnel = innerReader.readObject();
                    String host = tunnel.getString("tcpHost", "");
                    int port = tunnel.getInt("tcpPort", 0);

                    if (!host.isBlank() && port > 0) {
                        enderecoEmCache = new InetSocketAddress(host, port);
                        ultimaConsultaGist = Instant.now();
                        LOG.infof("Endereço TCP dinâmico obtido do Gist: %s:%d", host, port);
                        return enderecoEmCache;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warnf("Erro ao consultar GitHub Gist: %s", e.getMessage());
        }

        return enderecoEmCache;
    }
}