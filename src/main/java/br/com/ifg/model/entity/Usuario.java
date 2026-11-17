package br.com.ifg.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Getter @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Getter @Setter
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    @Getter @Setter
    private String email;

    // Tamanho atualizado para 255 para suportar hashes BCrypt completos e corresponder ao Script.sql
    @Column(nullable = false, length = 255)
    @Getter @Setter
    private String senha;

    @Column(name = "caminho_foto", columnDefinition = "TEXT")
    @Getter @Setter
    private String caminhoFoto;

    @Column(name = "codigo_recuperacao", length = 6)
    @Getter @Setter
    private String codigoRecuperacao;

    @Column(name = "codigo_expiracao")
    @Getter @Setter
    private LocalDateTime codigoExpiracao;

    @Column(name = "is_admin")
    @Getter @Setter
    private Boolean isAdmin = false;

    @ManyToOne
    @JoinColumn(name = "plano_id")
    @Getter @Setter
    private Plano plano;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter @Setter
    private List<Historico> filmesAssistidos;
}