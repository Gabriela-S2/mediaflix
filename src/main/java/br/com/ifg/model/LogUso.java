package br.com.ifg.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
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
}