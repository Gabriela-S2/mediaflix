package br.com.ifg.dto;

// DTO para exibir o Plano
public record PlanoResponseDTO(
        Long id,
        String nome,
        Integer nivel,
        Boolean temAnuncios
) {}