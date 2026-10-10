package es.lesiones.liga.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo cerrado de tipos de lesión. "Misma lesión" significa "mismo registro de esta tabla":
 * así las comparativas de recuperación son un simple GROUP BY y no dependen de cómo lo
 * escriba cada club o cada periódico.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"zona_codigo", "nombre"}))
public class TipoLesion {

    public enum Categoria { MUSCULAR, LIGAMENTOSA, OSEA, TENDINOSA, ARTICULAR, CONTUSION, OTRA }

    public enum Gravedad { LEVE, MODERADA, GRAVE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zona_codigo")
    private ZonaCorporal zona;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gravedad gravedad;
}
