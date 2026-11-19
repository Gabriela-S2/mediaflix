package br.com.ifg.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    private static final String FOTO_PADRAO = "/midia/novo_usuario.png";

    @Column(name = "caminho_foto", columnDefinition = "TEXT")
    private String caminhoFoto = FOTO_PADRAO;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(name = "telemetria_ativa", columnDefinition = "boolean default true")
    private Boolean telemetriaAtiva = true;

    @Column(name = "data_consentimento_lgpd")
    private java.time.LocalDateTime dataConsentimentoLgpd = java.time.LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id")
    private Plano plano;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Historico> filmesAssistidos;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_lista",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "filme_id")
    )
    private Set<Filme> minhaLista = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_curtidas",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "filme_id")
    )
    private Set<Filme> filmesCurtidos = new HashSet<>();
}