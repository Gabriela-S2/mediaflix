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
    JsonWebToken jwt;

    @Transactional
    public HistoricoResponseDTO registrarProgresso(HistoricoRequestDTO dto) {
        Usuario usuario = obterUsuarioLogado();
        Filme filme = filmeDAO.findById(dto.filmeId());

        historicoBO.validarRegistroProgresso(usuario, filme, dto.tempoAssistido());

        HistoricoId id = new HistoricoId(usuario.getId(), filme.getId());
        Historico historico = historicoDAO.findById(id);

        if (historico == null) {
            historico = new Historico();
            historico.setId(id);
            historico.setUsuario(usuario);
            historico.setFilme(filme);
        }

        historico.setTempoAssistido(dto.tempoAssistido());
        historicoDAO.persist(historico);

        LOG.debugf("Progresso atualizado: usuario=%s, filmeId=%d, segundos=%d",
                usuario.getEmail(), filme.getId(), dto.tempoAssistido());

        return converterParaDTO(historico);
    }

    public HistoricoResponseDTO buscarPorFilme(Long filmeId) {
        Usuario usuario = obterUsuarioLogado();
        Historico historico = historicoDAO.findById(new HistoricoId(usuario.getId(), filmeId));
        if (historico == null) {
            return new HistoricoResponseDTO(filmeId, null, null, 0L);
        }
        return converterParaDTO(historico);
    }

    public List<HistoricoResponseDTO> listarMeuHistorico() {
        Usuario usuario = obterUsuarioLogado();
        return historicoDAO.listarPorUsuario(usuario.getId()).stream()
                .map(this::converterParaDTO)
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

    private HistoricoResponseDTO converterParaDTO(Historico h) {
        return new HistoricoResponseDTO(
                h.getFilme().getId(),
                h.getFilme().getTitulo(),
                h.getFilme().getCaminhoImagemHorizontal(),
                h.getTempoAssistido()
        );
    }
}