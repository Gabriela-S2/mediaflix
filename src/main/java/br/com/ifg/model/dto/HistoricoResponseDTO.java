// HistoricoResponseDTO.java
package br.com.ifg.model.dto;

public record HistoricoResponseDTO(
        Long filmeId,
        String tituloFilme,
        String caminhoImagemHorizontal,
        Long tempoAssistido
) {}