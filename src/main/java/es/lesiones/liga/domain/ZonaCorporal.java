package es.lesiones.liga.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo cerrado de zonas del cuerpo. El {@code codigo} es el mismo identificador que usa
 * el mapa SVG del frontend (data-zona), así que la tabla y el dibujo siempre van de la mano.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ZonaCorporal {

    public enum Vista { FRONTAL, POSTERIOR, AMBAS }

    @Id
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Vista vista;

    /** true si existe lado derecho e izquierdo (rodilla, isquiotibial...). */
    private boolean bilateral;

    private int orden;
}
