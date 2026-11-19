package br.com.ifg.model.dto;

import java.util.List;

public record FilmeResponseDTO(
        Long id,
        String titulo,
        String descricao,
        String tipo,
        String genero,
        List<String> generos,
        String caminhoImagemHorizontal,
        String caminhoImagemVertical,
        Long planoId
) {}