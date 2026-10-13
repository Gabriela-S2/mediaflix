package br.com.ifg.model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class HistoricoId implements Serializable {

    private Long usuarioId;
    private Long filmeId;

    public HistoricoId() {}

    public HistoricoId(Long usuarioId, Long filmeId) {
        this.usuarioId = usuarioId;
        this.filmeId = filmeId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getFilmeId() {
        return filmeId;
    }

    public void setFilmeId(Long filmeId) {
        this.filmeId = filmeId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HistoricoId that = (HistoricoId) o;
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(filmeId, that.filmeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, filmeId);
    }
}