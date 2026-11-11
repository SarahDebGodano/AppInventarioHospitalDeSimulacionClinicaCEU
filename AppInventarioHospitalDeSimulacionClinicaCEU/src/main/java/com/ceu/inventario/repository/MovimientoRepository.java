package com.ceu.inventario.repository;

import com.ceu.inventario.modelo.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento,Long> {
    /** Los 100 últimos movimientos de un almacén, del más reciente al más antiguo. */
    List<Movimiento> findTop100ByAlmacenIdOrderByFechaDesc(Long almacenId);
}
