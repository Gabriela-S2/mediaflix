package br.com.ifg.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}