package br.com.ifg.model.dto;

public record HistoricoResponseDTO(
        Long filmeId,
        String tituloFilme,
        String tipo,
        String caminhoImagemHorizontal,
        Long tempoAssistido,
        Long duracaoTotal,
        Integer percentualAssistido
) {}