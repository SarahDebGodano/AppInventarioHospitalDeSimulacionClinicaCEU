package com.ceu.inventario.modelo;
import com.ceu.inventario.service.StockInsuficienteException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "existencia")
@Getter @Setter @NoArgsConstructor
public class Existencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_id")
    private Ubicacion ubicacion;   // puede ser null

    @Column(name = "cantidad", nullable = false)
    private int cantidad = 0;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo = 0;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    /** Se ejecuta solo antes de guardar: apunta la fecha del último cambio. */
    @PrePersist
    @PreUpdate
    void marcarActualizado() {
        actualizadoEn = LocalDateTime.now();
    }

    public void sumar(int n) {
        if (n <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        cantidad += n;
    }

    public void restar(int n) {
        if (n <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        if (n > cantidad) {
            throw new StockInsuficienteException(
                    "Solo quedan " + cantidad + " unidades de " + producto.getNombre());
        }
        cantidad -= n;
    }

    public boolean estaBajoMinimo() {
        return cantidad < stockMinimo;
    }
}
