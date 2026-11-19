package br.com.ifg.model.bo;

import br.com.ifg.model.dto.HistoricoResponseDTO;
import br.com.ifg.model.entity.Filme;
import br.com.ifg.model.entity.Historico;
import br.com.ifg.model.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HistoricoBO {

    @Inject
    FilmeBO filmeBO;

    public void validarRegistroProgresso(Usuario usuario, Filme filme, Long tempoAssistido) {
        if (filme == null) {
            throw new IllegalArgumentException("Filme não encontrado.");
        }
        if (tempoAssistido == null || tempoAssistido < 0) {
            throw new IllegalArgumentException("O tempo assistido não pode ser negativo.");
        }
        filmeBO.validarAcessoAoFilme(usuario, filme);
    }

    public int calcularPercentualAssistido(Long tempoAssistido, Long duracaoTotal) {
        if (tempoAssistido == null || tempoAssistido <= 0) {
            return 0;
        }
        if (duracaoTotal == null || duracaoTotal <= 0) {
            return 15; // Fallback visual caso a duração ainda não tenha sido registada
        }
        int percentual = (int) Math.round((tempoAssistido * 100.0) / duracaoTotal);
        return Math.max(3, Math.min(100, percentual));
    }

    public HistoricoResponseDTO converterParaDTO(Historico h) {
        Filme filme = h.getFilme();
        int percentual = calcularPercentualAssistido(h.getTempoAssistido(), h.getDuracaoTotal());
        return new HistoricoResponseDTO(
                filme.getId(),
                filme.getTitulo(),
                filme.getTipo() != null ? filme.getTipo() : "FILME",
                filme.getCaminhoImagemHorizontal(),
                h.getTempoAssistido(),
                h.getDuracaoTotal(),
                percentual
        );
    }
}