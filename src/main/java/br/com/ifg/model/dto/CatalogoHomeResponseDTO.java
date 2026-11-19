package br.com.ifg.model.dto;

import java.util.List;
import java.util.Map;

public record CatalogoHomeResponseDTO(
        List<FilmeResponseDTO> sugeridos,
        List<String> generosPreferidos,
        List<FilmeResponseDTO> minhaLista,
        List<FilmeResponseDTO> filmes,
        List<FilmeResponseDTO> series,
        Map<String, List<FilmeResponseDTO>> porGenero
) {}