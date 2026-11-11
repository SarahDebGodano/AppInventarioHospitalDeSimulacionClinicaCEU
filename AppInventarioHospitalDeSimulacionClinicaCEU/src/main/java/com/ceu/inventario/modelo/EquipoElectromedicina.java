package com.ceu.inventario.modelo;
import com.ceu.inventario.modelo.enums.EstadoEquipo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "equipo_electromedicina")
@Getter @Setter @NoArgsConstructor
public class EquipoElectromedicina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "numero_serie", nullable = false, unique = true, length = 100)
    private String numeroSerie;

    @Enumerated(EnumType.STRING)   // guarda "OPERATIVO", "AVERIADO" o "NECESITA_REVISION"
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEquipo estado = EstadoEquipo.OPERATIVO;

    @Column(name = "ultima_revision")
    private LocalDate ultimaRevision;
}
