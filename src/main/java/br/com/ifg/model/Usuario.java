package br.com.ifg.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCaminhoFoto() {
        return caminhoFoto;
    }

    public void setCaminhoFoto(String caminhoFoto) {
        this.caminhoFoto = caminhoFoto;
    }

    public String getCodigoRecuperacao() {
        return codigoRecuperacao;
    }

    public void setCodigoRecuperacao(String codigoRecuperacao) {
        this.codigoRecuperacao = codigoRecuperacao;
    }

    public LocalDateTime getCodigoExpiracao() {
        return codigoExpiracao;
    }

    public void setCodigoExpiracao(LocalDateTime codigoExpiracao) {
        this.codigoExpiracao = codigoExpiracao;
    }

    public Boolean getAdmin() {
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public Plano getPlano() {
        return plano;
    }

    public void setPlano(Plano plano) {
        this.plano = plano;
    }

    public List<Historico> getFilmesAssistidos() {
        return filmesAssistidos;
    }

    public void setFilmesAssistidos(List<Historico> filmesAssistidos) {
        this.filmesAssistidos = filmesAssistidos;
    }
}