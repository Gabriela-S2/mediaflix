package br.com.ifg.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String senha;

    @Column(name = "caminho_foto", columnDefinition = "TEXT")
    private String caminhoFoto;

    @Column(name = "codigo_recuperacao", length = 6)
    private String codigoRecuperacao;

    @Column(name = "codigo_expiracao")
    private LocalDateTime codigoExpiracao;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @ManyToOne
    @JoinColumn(name = "plano_id")
    private Plano plano;

    // Relacionamento com o histórico de filmes assistidos
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Historico> filmesAssistidos;
}