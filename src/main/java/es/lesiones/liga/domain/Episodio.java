package es.lesiones.liga.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Un episodio de lesión: un jugador, un tipo de lesión (y lado), una fecha de inicio y,
 * cuando se recupera, una fecha de alta. Mientras {@code fechaAlta} sea null, la lesión sigue activa.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Episodio {

    public enum Lado { DERECHO, IZQUIERDO, NO_APLICA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_id")
    private TipoLesion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Lado lado;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    /** null = sigue lesionado. */
    private LocalDate fechaAlta;

    private Integer partidosPerdidos;

    @Column(nullable = false)
    private boolean recaida;

    @Column(nullable = false)
    private boolean cirugia;

    /** Diagnóstico tal como lo publica el club, en texto corto (no se copian artículos). */
    @Column(length = 500)
    private String diagnosticoClub;

    /** Plazo de recuperación anunciado por el club, en días, para contrastarlo con el real. */
    private Integer plazoEstimadoDias;

    @Column(nullable = false, length = 500)
    private String fuenteUrl;

    private String fuenteNombre;

    private LocalDate fechaFuente;

    @Column(length = 1000)
    private String notas;

    @Column(nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    @PreUpdate
    void marcarActualizacion() {
        this.actualizadoEn = Instant.now();
    }
}
