package br.com.ifg.model;

import jakarta.persistence.*;



@Entity
@Table(name = "filme")
public class Filme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 50)
    private String genero;

    @Column(name = "caminho_imagem_horizontal")
    private String caminhoImagemHorizontal;

    @Column(name = "caminho_imagem_vertical")
    private String caminhoImagemVertical;

    @ManyToOne
    @JoinColumn(name = "plano_id", nullable = false)
    private Plano plano;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getCaminhoImagemHorizontal() {
        return caminhoImagemHorizontal;
    }

    public void setCaminhoImagemHorizontal(String caminhoImagemHorizontal) {
        this.caminhoImagemHorizontal = caminhoImagemHorizontal;
    }

    public String getCaminhoImagemVertical() {
        return caminhoImagemVertical;
    }

    public void setCaminhoImagemVertical(String caminhoImagemVertical) {
        this.caminhoImagemVertical = caminhoImagemVertical;
    }

    public Plano getPlano() {
        return plano;
    }

    public void setPlano(Plano plano) {
        this.plano = plano;
    }
}