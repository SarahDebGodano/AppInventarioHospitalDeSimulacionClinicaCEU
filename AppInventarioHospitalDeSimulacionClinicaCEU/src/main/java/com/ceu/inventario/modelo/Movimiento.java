package com.ceu.inventario.modelo;
import com.ceu.inventario.modelo.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento")
@Getter @Setter @NoArgsConstructor

public class Movimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMovimiento tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_destino_id")
    private Almacen almacenDestino;   // solo en traslados

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void ponerFecha() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
