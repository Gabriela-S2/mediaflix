package br.com.ifg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class HistoricoId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "filme_id")
    private Long filmeId;
}