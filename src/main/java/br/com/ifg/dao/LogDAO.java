package br.com.ifg.dao;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilitário centralizado de log do sistema Steam Clone.
 *
 * Gera arquivos por sessão na pasta "logs/YYYY-MM-DD/" dentro do diretório de execução:
 * - logs/YYYY-MM-DD/uso_HH-mm-ss.txt    → ações normais do sistema
 * - logs/YYYY-MM-DD/erros_HH-mm-ss.txt  → exceções e falhas capturadas
 */
public class LogDAO {

    // -------------------------------------------------------------------------
    // Configuração
    // -------------------------------------------------------------------------

    /** Pasta principal onde os arquivos de log serão criados. */
    private static final String PASTA_LOGS = "logs";

    /** Formato de timestamp usado dentro da linha de log (ex: 02/06/2026 14:30:05) */
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Captura a data e a hora do momento exato em que a aplicação é iniciada (Sessão)
    private static final String DATA_HOJE = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    private static final String HORA_SESSAO = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));

    // Caminho para a pasta de hoje (ex: logs/2026-06-23)
    private static final String PASTA_DIARIA = PASTA_LOGS + "/" + DATA_HOJE;

    // -------------------------------------------------------------------------
    // Inicialização da pasta de logs
    // -------------------------------------------------------------------------

    static {
        try {
            // Cria a pasta principal e a subpasta com a data de hoje, se não existirem
            Path pasta = Paths.get(PASTA_DIARIA);
            if (!Files.exists(pasta)) {
                Files.createDirectories(pasta);
            }
        } catch (IOException e) {
            System.err.println("[LogUtil] Não foi possível criar a pasta de logs: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Registra uma ação normal do sistema no arquivo de uso da sessão.
     */
    public static void registrarUso(String origem, String mensagem) {
        String linha = formatarLinha("USO", origem, mensagem);
        System.out.println(linha); // Mantém no console para facilitar

        String caminhoArquivo = obterCaminhoArquivo("uso");
        gravarNoArquivo(caminhoArquivo, linha);
    }

    /**
     * Registra uma exceção no arquivo de erros da sessão.
     */
    public static void registrarErro(String origem, String mensagem, Exception e) {
        String detalhe = (e != null) ? e.getMessage() : "sem detalhe";
        String linha = formatarLinha("ERRO", origem, mensagem + " | Causa: " + detalhe);
        System.err.println(linha); // Console em vermelho

        String caminhoArquivo = obterCaminhoArquivo("erros");
        gravarNoArquivo(caminhoArquivo, linha);

        // Grava também o stack trace completo no mesmo arquivo
        if (e != null) {
            gravarStackTrace(caminhoArquivo, e);
        }
    }

    /**
     * Registra uma exceção no arquivo de erros da sessão (forma simplificada).
     */
    public static void registrarErro(String origem, Exception e) {
        registrarErro(origem, "Exceção capturada", e);
    }

    public static void registrarAutenticacao(String username, String acao) {
        registrarUso("Autenticação", "Usuário '" + username + "' — " + acao);
    }

    public static void registrarNavegacao(String origem, String destino) {
        registrarUso("Navegação", "Tela: " + origem + " → " + destino);
    }

    // -------------------------------------------------------------------------
    // Métodos internos
    // -------------------------------------------------------------------------

    /**
     * Monta o nome do arquivo dinamicamente usando a pasta do dia e a hora da sessão.
     * Ex: "logs/2026-06-23/uso_17-57-00.txt"
     */
    private static String obterCaminhoArquivo(String prefixo) {
        return PASTA_DIARIA + "/" + prefixo + "_" + HORA_SESSAO + ".txt";
    }

    /**
     * Formata uma linha de log padronizada com a hora exata da emissão no início:
     * [dd/MM/yyyy HH:mm:ss] [TIPO] [origem] mensagem
     */
    private static String formatarLinha(String tipo, String origem, String mensagem) {
        String timestamp = LocalDateTime.now().format(FORMATO_HORA);
        return String.format("[%s] [%s] [%s] %s", timestamp, tipo, origem, mensagem);
    }

    /**
     * Abre o arquivo recebido em modo append e grava a linha.
     * Cria o arquivo se não existir.
     */
    private static synchronized void gravarNoArquivo(String caminho, String linha) {
        try (FileWriter fw = new FileWriter(caminho, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(linha);
        } catch (IOException e) {
            System.err.println("[LogUtil] Falha ao gravar log em '" + caminho + "': " + e.getMessage());
        }
    }

    /**
     * Grava o stack trace completo da exceção no arquivo de erros.
     */
    private static synchronized void gravarStackTrace(String caminho, Exception e) {
        try (FileWriter fw = new FileWriter(caminho, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("--- Início do Stack Trace ---");
            e.printStackTrace(pw);
            pw.println("--- Fim do Stack Trace ---");
        } catch (IOException ex) {
            System.err.println("[LogUtil] Falha ao gravar stack trace: " + ex.getMessage());
        }
    }
}