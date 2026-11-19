package br.com.ifg.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HistoricoRequestDTO(
        @NotNull(message = "O ID do filme é obrigatório")
        Long filmeId,

        @NotNull(message = "O tempo assistido é obrigatório")
        @Min(value = 0, message = "O tempo assistido não pode ser negativo")
        Long tempoAssistido,

        @Min(value = 0, message = "A duração total não pode ser negativa")
        Long duracaoTotal
) {}