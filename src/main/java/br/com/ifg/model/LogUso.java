package br.com.ifg.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_uso")
public class LogUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "acao_executada")
    private String acaoExecutada;

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    private String url;

    @Column(name = "ip_cliente", length = 45)
    private String ipCliente;

    @Column(name = "usuario_executor")
    private String usuarioExecutor;

    @Column(name = "data_hora")
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String headers;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAcaoExecutada() {
        return acaoExecutada;
    }

    public void setAcaoExecutada(String acaoExecutada) {
        this.acaoExecutada = acaoExecutada;
    }

    public String getMetodoHttp() {
        return metodoHttp;
    }

    public void setMetodoHttp(String metodoHttp) {
        this.metodoHttp = metodoHttp;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIpCliente() {
        return ipCliente;
    }

    public void setIpCliente(String ipCliente) {
        this.ipCliente = ipCliente;
    }

    public String getUsuarioExecutor() {
        return usuarioExecutor;
    }

    public void setUsuarioExecutor(String usuarioExecutor) {
        this.usuarioExecutor = usuarioExecutor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }
}