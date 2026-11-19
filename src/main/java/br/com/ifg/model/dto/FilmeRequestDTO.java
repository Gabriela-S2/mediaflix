package br.com.ifg.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FilmeRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotBlank(message = "O gênero é obrigatório")
        @Size(max = 50, message = "O gênero deve ter no máximo 50 caracteres")
        String genero,

        String caminhoImagemHorizontal,
        String caminhoImagemVertical,

        @NotNull(message = "O plano mínimo é obrigatório")
        Long planoId
) {}