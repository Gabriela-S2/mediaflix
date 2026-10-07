package br.com.ifg.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}