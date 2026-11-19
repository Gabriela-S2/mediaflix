package br.com.ifg.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilRequestDTO(
        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(min = 3, max = 100, message = "O nome de usuário deve ter entre 3 e 100 caracteres")
        String username,

        String caminhoFoto,

        @NotNull(message = "O plano é obrigatório")
        Long planoId
) {}