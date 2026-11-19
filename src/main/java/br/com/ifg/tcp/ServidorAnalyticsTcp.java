package br.com.ifg.tcp;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ServidorAnalyticsTcp {

    private static final Path ARQUIVO_DADOS = Paths.get("analytics-dados.json");
    private static final Pattern REGEX_PINGGY_TCP = Pattern.compile("tcp://([a-zA-Z0-9.-]+):(\\d+)");
    private static final Pattern REGEX_PINGGY_ID = Pattern.compile("Tunnel started \\(ID:\\s*([a-zA-Z0-9-]+)\\)");
    private static final Pattern REGEX_NGROK_HTTP = Pattern.compile("url=(https://[a-zA-Z0-9.-]+\\.ngrok[^\\s\"]*)");
    private static final Pattern REGEX_ANSI = Pattern.compile("\\u001B\\[[;\\d]*[ -/]*[@-~]");

    private static final Map<Long, Integer> visualizacoesPorFilme = new ConcurrentHashMap<>();
    private static final Map<Long, Set<String>> curtidasPorFilme = new ConcurrentHashMap<>();
    private static final Object LOCK_PERSISTENCIA = new Object();

    private static volatile boolean rodando = true;
    private static volatile String pinggyTunnelIdAtual = "";
    private static volatile String tcpHostAtual = "";
    private static volatile int tcpPortAtual = 0;
    private static volatile String httpUrlAtual = "";

    private static String gistId;
    private static String githubToken;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        gistId = obterConfig(dotenv, "GIST_ID", "");
        githubToken = obterConfig(dotenv, "GITHUB_TOKEN", "");
        int portaTcp = Integer.parseInt(obterConfig(dotenv, "TCP_SERVER_PORT", "9090"));
        String comandoPinggy = obterConfig(dotenv, "PINGGY_CMD", "pinggy --type tcp -l " + portaTcp + " --b");
        boolean ngrokAtivo = Boolean.parseBoolean(obterConfig(dotenv, "NGROK_ENABLED", "true"));
        String comandoNgrok = obterConfig(dotenv, "NGROK_CMD", "ngrok http 127.0.0.1:8080 --log=stdout");

        carregarDadosDoDisco();

        List<Process> processosFilhos = Collections.synchronizedList(new ArrayList<>());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            rodando = false;
            synchronized (processosFilhos) {
                for (Process p : processosFilhos) {
                    if (p.isAlive()) p.destroyForcibly();
                }
            }
            encerrarTunelPinggyAtual();
            System.out.println("[SERVIDOR] Encerrado com segurança.");
        }));

        // 1. Thread para iniciar/monitorar o túnel Pinggy CLI (--type tcp --b) e atualizar o Gist
        Thread threadPinggy = new Thread(() -> executarLoopPinggy(comandoPinggy, processosFilhos), "pinggy-mirror");
        threadPinggy.setDaemon(true);
        threadPinggy.start();

        // 2. Thread opcional para espelhar/sincronizar o túnel HTTP ngrok
        if (ngrokAtivo) {
            Thread threadNgrok = new Thread(() -> executarLoopNgrok(comandoNgrok, processosFilhos), "ngrok-mirror");
            threadNgrok.setDaemon(true);
            threadNgrok.start();
        }

        // 3. Servidor TCP principal
        ExecutorService poolClientes = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(portaTcp)) {
            System.out.println("==========================================================");
            System.out.println("[TCP-SERVER] Iniciado na porta local: " + portaTcp);
            System.out.println("[TCP-SERVER] Persistência ativa em: " + ARQUIVO_DADOS.toAbsolutePath());
            System.out.println("==========================================================");

            while (rodando) {
                Socket cliente = serverSocket.accept();
                poolClientes.submit(() -> processarClienteTcp(cliente));
            }
        } catch (IOException e) {
            System.err.println("[TCP-SERVER] Erro fatal no socket: " + e.getMessage());
        }
    }

    private static void processarClienteTcp(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String linha = in.readLine();
            if (linha == null || linha.isBlank()) return;

            try (JsonReader reader = Json.createReader(new StringReader(linha))) {
                JsonObject json = reader.readObject();
                String comando = json.getString("comando", "");

                if ("EVENTO".equalsIgnoreCase(comando)) {
                    String tipo = json.getString("tipo", "");
                    long filmeId = json.getJsonNumber("filmeId").longValue();
                    String titulo = json.getString("titulo", "Desconhecido");
                    String tokenUsuario = json.getString("tokenUsuario", "anonimo");

                    registrarEvento(tipo, filmeId, titulo, tokenUsuario);
                    out.println("OK");
                } else if ("RANKING_GLOBAL".equalsIgnoreCase(comando)) {
                    out.println(gerarRankingGlobalFormatado());
                }
            }
        } catch (Exception e) {
            System.err.println("[TCP-SERVER] Erro ao processar requisição: " + e.getMessage());
        }
    }

    private static void registrarEvento(String tipo, long filmeId, String titulo, String tokenUsuario) {
        switch (tipo.toUpperCase(Locale.ROOT)) {
            case "VIEW" -> {
                int total = visualizacoesPorFilme.merge(filmeId, 1, Integer::sum);
                System.out.printf("[MÉTRICAS-LGPD] +1 VIEW (anônimo) | Obra #%d (%s) | Total Views: %d%n",
                        filmeId, titulo, total);
            }
            case "LIKE" -> {
                Set<String> tokens = curtidasPorFilme.computeIfAbsent(filmeId, k -> ConcurrentHashMap.newKeySet());
                tokens.add(tokenUsuario);
                System.out.printf("[MÉTRICAS-LGPD] LIKE | Obra #%d (%s) | Token: %s | Total Likes: %d%n",
                        filmeId, titulo, tokenUsuario, tokens.size());
            }
            case "UNLIKE" -> {
                Set<String> tokens = curtidasPorFilme.get(filmeId);
                if (tokens != null) tokens.remove(tokenUsuario);
                int totalLikes = tokens != null ? tokens.size() : 0;
                System.out.printf("[MÉTRICAS-LGPD] UNLIKE | Obra #%d (%s) | Token: %s | Total Likes: %d%n",
                        filmeId, titulo, tokenUsuario, totalLikes);
            }
        }
        salvarDadosNoDisco();
    }

    private static String gerarRankingGlobalFormatado() {
        Set<Long> todosIds = new HashSet<>();
        todosIds.addAll(visualizacoesPorFilme.keySet());
        todosIds.addAll(curtidasPorFilme.keySet());

        return todosIds.stream()
                .map(id -> {
                    int views = visualizacoesPorFilme.getOrDefault(id, 0);
                    int likes = curtidasPorFilme.getOrDefault(id, Set.of()).size();
                    double score = (views * 1.0) + (likes * 3.0);
                    return score > 0 ? id + ":" + score : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    // =========================================================================
    // PINGGY CLI (--type tcp --b) + MONITORAMENTO DE SAÚDE + GIST
    // =========================================================================
    private static void executarLoopPinggy(String comando, List<Process> processosFilhos) {
        while (rodando) {
            try {
                System.out.println("[PINGGY] Executando: " + comando);
                ProcessBuilder pb = criarProcessBuilderMultiplataforma(comando);
                pb.redirectErrorStream(true);
                Process processo = pb.start();
                processosFilhos.add(processo);

                boolean urlCapturadaNesteCiclo = false;

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        String limpa = REGEX_ANSI.matcher(linha).replaceAll("").trim();
                        if (!limpa.isEmpty() && !limpa.endsWith("Initializing...")) {
                            System.out.println("[PINGGY] " + limpa);

                            Matcher mId = REGEX_PINGGY_ID.matcher(limpa);
                            if (mId.find()) {
                                pinggyTunnelIdAtual = mId.group(1);
                            }

                            Matcher mTcp = REGEX_PINGGY_TCP.matcher(limpa);
                            if (mTcp.find()) {
                                String novoHost = mTcp.group(1);
                                int novaPorta = Integer.parseInt(mTcp.group(2));
                                urlCapturadaNesteCiclo = true;

                                if (!novoHost.equals(tcpHostAtual) || novaPorta != tcpPortAtual) {
                                    tcpHostAtual = novoHost;
                                    tcpPortAtual = novaPorta;
                                    System.out.printf("[GIST-SYNC] Túnel TCP Pinggy ativo em: %s:%d%n", tcpHostAtual, tcpPortAtual);
                                    atualizarGist();
                                }
                            }
                        }
                    }
                }

                int exitCode = processo.waitFor();
                processosFilhos.remove(processo);

                if (rodando && exitCode == 0 && urlCapturadaNesteCiclo && !tcpHostAtual.isBlank() && tcpPortAtual > 0) {
                    System.out.println("[PINGGY] Túnel TCP ativo em background (--b). Monitorando disponibilidade...");
                    aguardarEnquantoTunelEstiverAtivo(tcpHostAtual, tcpPortAtual);
                    if (rodando) {
                        System.out.println("[PINGGY] O túnel TCP anterior expirou ou caiu. Criando novo túnel...");
                        encerrarTunelPinggyAtual();
                    }
                } else if (rodando) {
                    System.out.println("[PINGGY] Processo encerrado (código " + exitCode + "). Tentando novamente em 5s...");
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                System.err.println("[PINGGY] Falha ao executar Pinggy CLI: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private static void aguardarEnquantoTunelEstiverAtivo(String host, int port) {
        int falhasConsecutivas = 0;
        while (rodando) {
            try {
                Thread.sleep(15000);
                try (Socket teste = new Socket()) {
                    teste.connect(new InetSocketAddress(host, port), 3000);
                    falhasConsecutivas = 0;
                }
            } catch (InterruptedException e) {
                return;
            } catch (IOException e) {
                falhasConsecutivas++;
                System.out.printf("[PINGGY] Falha na verificação do túnel %s:%d (%d/2)%n", host, port, falhasConsecutivas);
                if (falhasConsecutivas >= 2) {
                    return;
                }
            }
        }
    }

    private static void encerrarTunelPinggyAtual() {
        try {
            if (!pinggyTunnelIdAtual.isBlank()) {
                criarProcessBuilderMultiplataforma("pinggy stop " + pinggyTunnelIdAtual).start().waitFor();
                pinggyTunnelIdAtual = "";
            }
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // NGROK HTTP + DETECÇÃO DE INSTÂNCIA JÁ ATIVA (EVITA ERR_NGROK_334)
    // =========================================================================
    private static void executarLoopNgrok(String comando, List<Process> processosFilhos) {
        while (rodando) {
            try {
                // Se já existir um ngrok ativo na máquina (porta 4040), reaproveita a URL sem conflito
                String urlJaAtiva = consultarUrlNgrokLocal();
                if (urlJaAtiva != null && !urlJaAtiva.isBlank()) {
                    if (!urlJaAtiva.equals(httpUrlAtual)) {
                        httpUrlAtual = urlJaAtiva;
                        System.out.println("[NGROK] Túnel já ativo reaproveitado: " + httpUrlAtual);
                        atualizarGist();
                    }
                    Thread.sleep(15000);
                    continue;
                }

                System.out.println("[NGROK] Iniciando túnel HTTP: " + comando);
                ProcessBuilder pb = criarProcessBuilderMultiplataforma(comando);
                pb.redirectErrorStream(true);
                Process processo = pb.start();
                processosFilhos.add(processo);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        String limpa = REGEX_ANSI.matcher(linha).replaceAll("").trim();
                        if (!limpa.isEmpty()) {
                            System.out.println("[NGROK] " + limpa);
                            Matcher m = REGEX_NGROK_HTTP.matcher(limpa);
                            if (m.find()) {
                                String novaUrl = m.group(1);
                                if (!novaUrl.equals(httpUrlAtual)) {
                                    httpUrlAtual = novaUrl;
                                    System.out.println("[GIST-SYNC] Nova URL pública ngrok detectada: " + httpUrlAtual);
                                    atualizarGist();
                                }
                            }
                        }
                    }
                }

                int exitCode = processo.waitFor();
                processosFilhos.remove(processo);
                if (rodando) {
                    System.out.println("[NGROK] Túnel encerrado (código " + exitCode + "). Verificando em 5s...");
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                System.err.println("[NGROK] Falha ao executar ngrok: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private static String consultarUrlNgrokLocal() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:4040/api/tunnels"))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();

            HttpResponse<String> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                try (JsonReader reader = Json.createReader(new StringReader(res.body()))) {
                    JsonObject root = reader.readObject();
                    JsonArray tunnels = root.getJsonArray("tunnels");
                    if (tunnels != null) {
                        for (int i = 0; i < tunnels.size(); i++) {
                            String publicUrl = tunnels.getJsonObject(i).getString("public_url", "");
                            if (publicUrl.startsWith("https://")) {
                                return publicUrl;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ProcessBuilder criarProcessBuilderMultiplataforma(String comando) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", comando);
        }
        return new ProcessBuilder("sh", "-c", comando);
    }

    private static synchronized void atualizarGist() {
        if (gistId.isBlank() || githubToken.isBlank()) {
            System.err.println("[GIST-SYNC] GIST_ID ou GITHUB_TOKEN não configurados no .env.");
            return;
        }

        try {
            JsonObject conteudoTunnel = Json.createObjectBuilder()
                    .add("tcpHost", tcpHostAtual)
                    .add("tcpPort", tcpPortAtual)
                    .add("httpUrl", httpUrlAtual)
                    .add("atualizadoEm", LocalDateTime.now().toString())
                    .build();

            JsonObject payloadGist = Json.createObjectBuilder()
                    .add("description", "MediaFlix - Endereço Dinâmico Pinggy/ngrok")
                    .add("files", Json.createObjectBuilder()
                            .add("mediaflix-tunnel.json", Json.createObjectBuilder()
                                    .add("content", conteudoTunnel.toString())))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/gists/" + gistId))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + githubToken)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(payloadGist.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.printf("[GIST-SYNC] Gist atualizado com sucesso! TCP=%s:%d | HTTP=%s%n",
                        tcpHostAtual, tcpPortAtual, httpUrlAtual);
            } else {
                System.err.printf("[GIST-SYNC] Erro HTTP %d ao atualizar Gist: %s%n",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            System.err.println("[GIST-SYNC] Exceção ao atualizar Gist: " + e.getMessage());
        }
    }

    // =========================================================================
    // PERSISTÊNCIA LOCAL
    // =========================================================================
    private static void salvarDadosNoDisco() {
        synchronized (LOCK_PERSISTENCIA) {
            try {
                JsonObjectBuilder viewsBuilder = Json.createObjectBuilder();
                visualizacoesPorFilme.forEach((id, qtd) -> viewsBuilder.add(String.valueOf(id), qtd));

                JsonObjectBuilder likesBuilder = Json.createObjectBuilder();
                curtidasPorFilme.forEach((id, users) -> {
                    var arr = Json.createArrayBuilder();
                    users.forEach(arr::add);
                    likesBuilder.add(String.valueOf(id), arr);
                });

                JsonObject raiz = Json.createObjectBuilder()
                        .add("visualizacoes", viewsBuilder)
                        .add("curtidas", likesBuilder)
                        .build();

                Path temp = Paths.get("analytics-dados.json.tmp");
                Files.writeString(temp, raiz.toString(), StandardCharsets.UTF_8);
                Files.move(temp, ARQUIVO_DADOS, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                System.err.println("[PERSISTÊNCIA] Erro ao salvar métricas: " + e.getMessage());
            }
        }
    }

    private static void carregarDadosDoDisco() {
        if (!Files.exists(ARQUIVO_DADOS)) return;

        try (InputStream is = Files.newInputStream(ARQUIVO_DADOS);
             JsonReader reader = Json.createReader(is)) {
            JsonObject raiz = reader.readObject();

            if (raiz.containsKey("visualizacoes")) {
                JsonObject views = raiz.getJsonObject("visualizacoes");
                for (String key : views.keySet()) {
                    visualizacoesPorFilme.put(Long.parseLong(key), views.getInt(key));
                }
            }
            if (raiz.containsKey("curtidas")) {
                JsonObject likes = raiz.getJsonObject("curtidas");
                for (String key : likes.keySet()) {
                    Set<String> users = ConcurrentHashMap.newKeySet();
                    likes.getJsonArray(key).forEach(v -> users.add(v.toString().replace("\"", "")));
                    curtidasPorFilme.put(Long.parseLong(key), users);
                }
            }
            System.out.println("[PERSISTÊNCIA] Métricas restauradas: "
                    + visualizacoesPorFilme.size() + " obras com views, "
                    + curtidasPorFilme.size() + " obras com likes.");
        } catch (Exception e) {
            System.err.println("[PERSISTÊNCIA] Falha ao ler arquivo anterior: " + e.getMessage());
        }
    }

    private static String obterConfig(Dotenv dotenv, String chave, String padrao) {
        String env = System.getenv(chave);
        if (env != null && !env.isBlank()) return env;
        String dot = dotenv.get(chave);
        return (dot != null && !dot.isBlank()) ? dot : padrao;
    }
}