package br.com.ifg.service;

import br.com.ifg.model.LogUso;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class LogArquivoService {

    // Define a pasta onde os logs serão guardados na raiz do projeto
    private static final String DIRETORIO_LOGS = "logs";

    public void salvarLogEmArquivo(LogUso log) {
        try {
            // 1. Garantir que a pasta 'logs' existe
            Path diretorio = Paths.get(DIRETORIO_LOGS);
            if (!Files.exists(diretorio)) {
                Files.createDirectories(diretorio);
            }

            // 2. Formatar a data para o nome do ficheiro (ex: 2026-10-07)
            String dataAtual = log.getDataHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 3. Sanitizar o nome do utilizador para evitar problemas no sistema de ficheiros
            String nomeUsuario = log.getUsuarioExecutor().replaceAll("[^a-zA-Z0-9_-]", "_");

            // 4. Montar o nome do ficheiro (ex: 2026-10-07_joao_silva.txt)
            String nomeArquivo = String.format("%s_%s.txt", dataAtual, nomeUsuario);
            Path caminhoArquivo = diretorio.resolve(nomeArquivo);

            // 5. Formatar o conteúdo do log para o .txt
            String conteudoLog = formatarConteudoLog(log);

            // 6. Escrever no ficheiro (cria se não existir, e anexa no final se já existir)
            Files.writeString(caminhoArquivo, conteudoLog,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            // Em caso de erro na gravação, enviamos para a saída padrão de erros do servidor
            System.err.println("Erro ao gravar log no ficheiro TXT: " + e.getMessage());
        }
    }

    private String formatarConteudoLog(LogUso log) {
        return String.format(
                "[%s] %s %s\n" +
                        "- Utilizador: %s\n" +
                        "- IP do Cliente: %s\n" +
                        "- Ação (Método Java): %s\n" +
                        "- Headers:\n%s" +
                        "- Payload:\n%s\n" +
                        "--------------------------------------------------\n",
                log.getDataHora().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                log.getMetodoHttp(),
                log.getUrl(),
                log.getUsuarioExecutor(),
                log.getIpCliente(),
                log.getAcaoExecutada(),
                log.getHeaders(),
                log.getPayload()
        );
    }
}