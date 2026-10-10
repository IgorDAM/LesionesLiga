package es.lesiones.liga.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Club {

    public enum Division { PRIMERA, SEGUNDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Division division;

    /** Web oficial del club (opcional, se rellena a mano). */
    private String urlOficial;

    /** Página donde el club publica sus partes médicos (opcional). */
    private String urlParteMedico;

    /** Última vez que revisaste las fuentes de este club (flujo semanal). */
    private LocalDate ultimaRevision;
}
