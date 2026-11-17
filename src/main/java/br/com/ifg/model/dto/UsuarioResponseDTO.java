package br.com.ifg.model.dto;

// DTO para exibir os dados do Usuário (omitimos a senha propositalmente)
public record UsuarioResponseDTO(
        Long id,
        String username,
        String email,
        String caminhoFoto,
        Boolean isAdmin,
        PlanoResponseDTO plano
) {}