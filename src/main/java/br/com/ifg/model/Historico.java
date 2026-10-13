package br.com.ifg.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario_filmes")
public class Historico {

    @EmbeddedId
    private HistoricoId id = new HistoricoId();

    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @MapsId("filmeId")
    @JoinColumn(name = "filme_id")
    private Filme filme;

    @Column(name = "tempo_assistido")
    private Long tempoAssistido = 0L;

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
}