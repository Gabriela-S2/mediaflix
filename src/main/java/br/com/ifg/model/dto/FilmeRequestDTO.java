package br.com.ifg.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FilmeRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotBlank(message = "O tipo da obra (Filme ou Série) é obrigatório")
        String tipo,

        @NotEmpty(message = "Selecione pelo menos um gênero")
        List<String> generos,

        String caminhoImagemHorizontal,
        String caminhoImagemVertical,

        @NotNull(message = "O plano mínimo é obrigatório")
        Long planoId
) {}