package br.com.ifg.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponseDTO(
        int status,
        String erro,
        List<String> detalhes,
        LocalDateTime timestamp
) {
    public ErroResponseDTO(int status, String erro) {
        this(status, erro, List.of(), LocalDateTime.now());
    }
}