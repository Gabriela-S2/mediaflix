// FilmeResponseDTO.java
package br.com.ifg.model.dto;

public record FilmeResponseDTO(
        Long id,
        String titulo,
        String descricao,
        String genero,
        String caminhoImagemHorizontal,
        String caminhoImagemVertical,
        Long planoId
) {}