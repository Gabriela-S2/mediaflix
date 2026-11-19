package br.com.ifg.model.bo;

import br.com.ifg.model.dto.CatalogoHomeResponseDTO;
import br.com.ifg.model.dto.FilmeResponseDTO;
import br.com.ifg.model.entity.Filme;
import br.com.ifg.model.entity.Plano;
import br.com.ifg.model.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class FilmeBO {

    public static final List<String> GENEROS_OFICIAIS = List.of(
            "Ação",
            "Comédia",
            "Drama",
            "Fantasia",
            "Ficção científica",
            "Romance",
            "Terror",
            "Musical"
    );

    private static final Set<String> TIPOS_MIDIA_PERMITIDOS = Set.of("FILME", "SERIE");

    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    public String validarENormalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("O tipo da obra (FILME ou SERIE) é obrigatório.");
        }
        String normalizado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_MIDIA_PERMITIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Tipo inválido. Escolha entre Filme ou Série.");
        }
        return normalizado;
    }

    public String validarEFormatarGeneros(List<String> generosSelecionados) {
        if (generosSelecionados == null || generosSelecionados.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um gênero.");
        }

        List<String> validados = new ArrayList<>();
        for (String g : generosSelecionados) {
            if (g == null || g.isBlank()) continue;
            String encontrado = GENEROS_OFICIAIS.stream()
                    .filter(oficial -> oficial.equalsIgnoreCase(g.trim()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Gênero inválido: " + g));
            if (!validados.contains(encontrado)) {
                validados.add(encontrado);
            }
        }

        if (validados.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um gênero válido.");
        }

        return String.join(", ", validados);
    }

    public void validarAcessoAoFilme(Usuario usuario, Filme filme) {
        if (Boolean.TRUE.equals(usuario.getIsAdmin())) {
            return;
        }
        if (filme.getPlano() != null) {
            if (usuario.getPlano() == null || filme.getPlano().getNivel() > usuario.getPlano().getNivel()) {
                throw new SecurityException("O seu plano atual não permite assistir a este título.");
            }
        }
    }

    public Plano validarPlanoExistente(Plano plano) {
        if (plano == null) {
            throw new IllegalArgumentException("Plano selecionado inválido.");
        }
        return plano;
    }

    /**
     * Constrói todas as secções do catálogo (Sugeridos, Minha Lista, Filmes, Séries e por Gênero).
     */
    public CatalogoHomeResponseDTO organizarCatalogoHome(
            List<Filme> catalogoPermitido,
            Set<Filme> minhaListaUsuario,
            Set<Filme> filmesCurtidosUsuario
    ) {
        Set<Long> idsPermitidos = catalogoPermitido.stream()
                .map(Filme::getId)
                .collect(Collectors.toSet());

        Set<Long> idsCurtidos = filmesCurtidosUsuario == null
                ? Set.of()
                : filmesCurtidosUsuario.stream().map(Filme::getId).collect(Collectors.toSet());

        // 1. Calcula pesos de preferência com base nos gêneros das obras curtidas
        Map<String, Integer> pesosPorGenero = new LinkedHashMap<>();
        if (filmesCurtidosUsuario != null) {
            for (Filme curtido : filmesCurtidosUsuario) {
                for (String genero : curtido.getGenerosLista()) {
                    pesosPorGenero.merge(genero, 1, Integer::sum);
                }
            }
        }

        List<String> generosPreferidos = pesosPorGenero.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        // 2. Sugeridos (apenas se o usuário tiver preferências registadas via like)
        List<FilmeResponseDTO> sugeridos = List.of();
        if (!pesosPorGenero.isEmpty()) {
            sugeridos = catalogoPermitido.stream()
                    .map(filme -> {
                        int afinidade = filme.getGenerosLista().stream()
                                .mapToInt(g -> pesosPorGenero.getOrDefault(g, 0))
                                .sum();
                        return Map.entry(filme, afinidade);
                    })
                    .filter(entry -> entry.getValue() > 0)
                    .sorted((a, b) -> {
                        // Prioriza maior afinidade de gêneros; em empate, prioriza títulos ainda não curtidos
                        int cmpScore = Integer.compare(b.getValue(), a.getValue());
                        if (cmpScore != 0) return cmpScore;
                        boolean aNaoCurtido = !idsCurtidos.contains(a.getKey().getId());
                        boolean bNaoCurtido = !idsCurtidos.contains(b.getKey().getId());
                        if (aNaoCurtido != bNaoCurtido) return aNaoCurtido ? -1 : 1;
                        return Long.compare(b.getKey().getId(), a.getKey().getId());
                    })
                    .map(entry -> converterParaDTO(entry.getKey()))
                    .toList();
        }

        // 3. Minha Lista (filtrada pelo plano atual do usuário)
        List<FilmeResponseDTO> minhaListaDTO = minhaListaUsuario == null
                ? List.of()
                : minhaListaUsuario.stream()
                .filter(f -> idsPermitidos.contains(f.getId()))
                .sorted(Comparator.comparing(Filme::getId))
                .map(this::converterParaDTO)
                .toList();

        // 4. Filmes e Séries
        List<FilmeResponseDTO> filmes = catalogoPermitido.stream()
                .filter(f -> !"SERIE".equalsIgnoreCase(f.getTipo()))
                .map(this::converterParaDTO)
                .toList();

        List<FilmeResponseDTO> series = catalogoPermitido.stream()
                .filter(f -> "SERIE".equalsIgnoreCase(f.getTipo()))
                .map(this::converterParaDTO)
                .toList();

        // 5. Secções baseadas nos gêneros disponíveis no catálogo permitido
        Map<String, List<FilmeResponseDTO>> porGenero = new LinkedHashMap<>();
        for (String generoOficial : GENEROS_OFICIAIS) {
            List<FilmeResponseDTO> doGenero = catalogoPermitido.stream()
                    .filter(f -> f.getGenerosLista().stream()
                            .anyMatch(g -> g.equalsIgnoreCase(generoOficial)))
                    .map(this::converterParaDTO)
                    .toList();

            if (!doGenero.isEmpty()) {
                porGenero.put(generoOficial, doGenero);
            }
        }

        return new CatalogoHomeResponseDTO(
                sugeridos,
                generosPreferidos,
                minhaListaDTO,
                filmes,
                series,
                porGenero
        );
    }

    public FilmeResponseDTO converterParaDTO(Filme filme) {
        return new FilmeResponseDTO(
                filme.getId(),
                filme.getTitulo(),
                filme.getDescricao(),
                filme.getTipo() != null ? filme.getTipo() : "FILME",
                filme.getGenero(),
                filme.getGenerosLista(),
                filme.getCaminhoImagemHorizontal(),
                filme.getCaminhoImagemVertical(),
                filme.getPlano() != null ? filme.getPlano().getId() : null
        );
    }

    public String sanitizarNomeArquivo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return "titulo";
        }
        String semAcentos = Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String slug = semAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return slug.isBlank() ? "titulo" : slug;
    }

    public String salvarImagemNoDisco(
            FileUpload arquivo,
            String tituloFilme,
            Path pastaDestino,
            String caminhoAnterior,
            String prefixoRotaPublica
    ) throws IOException {
        if (arquivo == null || arquivo.uploadedFile() == null || arquivo.size() == 0) {
            throw new IllegalArgumentException("Nenhum arquivo de imagem válido foi enviado.");
        }

        String contentType = arquivo.contentType() != null
                ? arquivo.contentType().toLowerCase(Locale.ROOT)
                : "";

        if (!TIPOS_IMAGEM_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Formato inválido. Envie apenas PNG, JPG ou WEBP.");
        }

        Files.createDirectories(pastaDestino);

        String nomeBaseSanitizado = sanitizarNomeArquivo(tituloFilme);
        String nomeArquivoFinal;
        Path caminhoCompleto;

        if ("image/webp".equals(contentType)) {
            validarCabecalhoWebp(arquivo.uploadedFile());
            nomeArquivoFinal = nomeBaseSanitizado + ".webp";
            caminhoCompleto = pastaDestino.resolve(nomeArquivoFinal).normalize();
            Files.copy(arquivo.uploadedFile(), caminhoCompleto, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else {
            BufferedImage imagem = ImageIO.read(arquivo.uploadedFile().toFile());
            if (imagem == null) {
                throw new IllegalArgumentException("O arquivo enviado não é uma imagem válida.");
            }
            nomeArquivoFinal = nomeBaseSanitizado + ".png";
            caminhoCompleto = pastaDestino.resolve(nomeArquivoFinal).normalize();
            ImageIO.write(imagem, "png", caminhoCompleto.toFile());
        }

        if (caminhoAnterior != null && caminhoAnterior.startsWith(prefixoRotaPublica)) {
            String arquivoAntigo = caminhoAnterior.substring(prefixoRotaPublica.length());
            if (!arquivoAntigo.equals(nomeArquivoFinal)) {
                removerArquivoSeLocal(caminhoAnterior, pastaDestino, prefixoRotaPublica);
            }
        }

        return prefixoRotaPublica + nomeArquivoFinal;
    }

    public void removerArquivoSeLocal(String urlImagem, Path pastaDestino, String prefixoRotaPublica) {
        if (urlImagem == null || !urlImagem.startsWith(prefixoRotaPublica)) {
            return;
        }
        try {
            String nomeArquivo = urlImagem.substring(prefixoRotaPublica.length());
            Path caminho = pastaDestino.resolve(nomeArquivo).normalize();
            if (caminho.startsWith(pastaDestino)) {
                Files.deleteIfExists(caminho);
            }
        } catch (IOException ignored) {
        }
    }

    private void validarCabecalhoWebp(Path arquivo) throws IOException {
        byte[] header = Files.readAllBytes(arquivo);
        boolean valido = header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        if (!valido) {
            throw new IllegalArgumentException("O arquivo WEBP enviado está corrompido ou é inválido.");
        }
    }
}