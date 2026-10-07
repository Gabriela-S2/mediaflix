package br.com.ifg.dto;

// DTO para exibir o Filme (já embute as regras de mídia e o plano exigido)
public record FilmeResponseDTO(
        Long id,
        String titulo,
        String descricao,
        String genero,
        String caminhoImagemHorizontal,
        String caminhoImagemVertical,
        PlanoResponseDTO planoMinimo
) {}