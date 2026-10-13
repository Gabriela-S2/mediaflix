package br.com.ifg.model;

import jakarta.persistence.*;

@Entity
@Table(name = "planos")
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    @Column(nullable = false, unique = true)
    private Integer nivel;

    @Column(name = "tem_anuncios")
    private Boolean temAnuncios = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public Boolean getTemAnuncios() {
        return temAnuncios;
    }

    public void setTemAnuncios(Boolean temAnuncios) {
        this.temAnuncios = temAnuncios;
    }
}