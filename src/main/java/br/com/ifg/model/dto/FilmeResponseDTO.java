package br.com.ifg.model.dto;

import lombok.Data;

@Data
public class FilmeResponseDTO {
    private Long id; // Campo adicionado para permitir o mapeamento no Service
    private String titulo;
    private String descricao;
    private String genero;
    private String caminhoImagemHorizontal;
    private String caminhoImagemVertical;
    private Long planoId;
}