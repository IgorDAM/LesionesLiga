package es.lesiones.liga.domain;

import es.lesiones.liga.util.Textos;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"nombre", "club_id"}))
public class Jugador {

    public enum Posicion { PORTERO, DEFENSA, CENTROCAMPISTA, DELANTERO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    /** Nombre en minúsculas y sin tildes; es lo que se consulta al buscar. */
    @Column(nullable = false)
    private String nombreBusqueda;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @Enumerated(EnumType.STRING)
    private Posicion posicion;

    private LocalDate fechaNacimiento;

    @PrePersist
    @PreUpdate
    void actualizarNombreBusqueda() {
        this.nombreBusqueda = Textos.normalizar(nombre);
    }
}
