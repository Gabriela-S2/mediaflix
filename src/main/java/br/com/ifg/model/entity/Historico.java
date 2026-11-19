package br.com.ifg.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_filmes")
public class Historico {

    @EmbeddedId
    private HistoricoId id = new HistoricoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("filmeId")
    @JoinColumn(name = "filme_id")
    private Filme filme;

    @Column(name = "tempo_assistido")
    private Long tempoAssistido = 0L;

    @Column(name = "duracao_total")
    private Long duracaoTotal = 0L;

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void atualizarTimestamp() {
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    public HistoricoId getId() {
        return id;
    }

    public void setId(HistoricoId id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Filme getFilme() {
        return filme;
    }

    public void setFilme(Filme filme) {
        this.filme = filme;
    }

    public Long getTempoAssistido() {
        return tempoAssistido;
    }

    public void setTempoAssistido(Long tempoAssistido) {
        this.tempoAssistido = tempoAssistido;
    }

    public Long getDuracaoTotal() {
        return duracaoTotal;
    }

    public void setDuracaoTotal(Long duracaoTotal) {
        this.duracaoTotal = duracaoTotal;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}