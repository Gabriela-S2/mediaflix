package br.com.ifg.model.dto;

public record InteracaoFilmeResponseDTO(
        Long filmeId,
        boolean naMinhaLista,
        boolean curtido
) {}