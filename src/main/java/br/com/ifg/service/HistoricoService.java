package br.com.ifg.service;

import br.com.ifg.model.bo.HistoricoBO;
import br.com.ifg.model.dao.FilmeDAO;
import br.com.ifg.model.dao.HistoricoDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.dto.HistoricoRequestDTO;
import br.com.ifg.model.dto.HistoricoResponseDTO;
import br.com.ifg.model.entity.Filme;
import br.com.ifg.model.entity.Historico;
import br.com.ifg.model.entity.HistoricoId;
import br.com.ifg.model.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class HistoricoService {

    private static final Logger LOG = Logger.getLogger(HistoricoService.class);

    @Inject
    HistoricoDAO historicoDAO;

    @Inject
    UsuarioDAO usuarioDAO;

    @Inject
    FilmeDAO filmeDAO;

    @Inject
    HistoricoBO historicoBO;

    @Inject
    TcpAnalyticsClient tcpAnalyticsClient;

    @Inject
    JsonWebToken jwt;

    @Transactional
    public HistoricoResponseDTO registrarProgresso(HistoricoRequestDTO dto) {
        Usuario usuario = obterUsuarioLogado();
        Filme filme = filmeDAO.findById(dto.filmeId());

        historicoBO.validarRegistroProgresso(usuario, filme, dto.tempoAssistido());

        HistoricoId id = new HistoricoId(usuario.getId(), filme.getId());
        Historico historico = historicoDAO.findById(id);
        boolean primeiraVisualizacao = false;

        if (historico == null) {
            historico = new Historico();
            historico.setId(id);
            historico.setUsuario(usuario);
            historico.setFilme(filme);
            primeiraVisualizacao = dto.tempoAssistido() > 0;
        } else if ((historico.getTempoAssistido() == null || historico.getTempoAssistido() == 0) && dto.tempoAssistido() > 0) {
            primeiraVisualizacao = true;
        }

        historico.setTempoAssistido(dto.tempoAssistido());
        if (dto.duracaoTotal() != null && dto.duracaoTotal() > 0) {
            historico.setDuracaoTotal(dto.duracaoTotal());
        }
        historico.setUltimaAtualizacao(LocalDateTime.now());

        historicoDAO.persist(historico);

        if (primeiraVisualizacao) {
            tcpAnalyticsClient.registrarEventoAsync("VIEW", filme.getId(), filme.getTitulo(), usuario.getEmail());
        }

        if (primeiraVisualizacao && !Boolean.FALSE.equals(usuario.getTelemetriaAtiva())) {
            tcpAnalyticsClient.registrarEventoAsync("VIEW", filme.getId(), filme.getTitulo(), usuario.getEmail());
        }

        LOG.debugf("Progresso atualizado: usuario=%s, filmeId=%d, segundos=%d/%d",
                usuario.getEmail(), filme.getId(), dto.tempoAssistido(), historico.getDuracaoTotal());

        return historicoBO.converterParaDTO(historico);
    }

    public HistoricoResponseDTO buscarPorFilme(Long filmeId) {
        Usuario usuario = obterUsuarioLogado();
        Historico historico = historicoDAO.findById(new HistoricoId(usuario.getId(), filmeId));
        if (historico == null) {
            return new HistoricoResponseDTO(filmeId, null, "FILME", null, 0L, 0L, 0);
        }
        return historicoBO.converterParaDTO(historico);
    }

    public List<HistoricoResponseDTO> listarMeuHistorico() {
        Usuario usuario = obterUsuarioLogado();
        return historicoDAO.listarPorUsuario(usuario.getId()).stream()
                .map(historicoBO::converterParaDTO)
                .toList();
    }

    private Usuario obterUsuarioLogado() {
        String email = jwt.getName();
        Usuario usuario = usuarioDAO.buscarPorEmail(email);
        if (usuario == null) {
            throw new SecurityException("Usuário não autenticado.");
        }
        return usuario;
    }
}