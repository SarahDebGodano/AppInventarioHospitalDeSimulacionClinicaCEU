package com.ceu.inventario.service;

import com.ceu.inventario.dto.AlmacenDTO;
import com.ceu.inventario.dto.ExistenciaDTO;
import com.ceu.inventario.dto.MovimientoDTO;
import com.ceu.inventario.dto.MovimientoRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventarioService {
    /** Los almacenes activos, ordenados por nombre. */
    public List<AlmacenDTO> listarAlmacenes() {
        throw new UnsupportedOperationException("Pendiente");
    }

    /**
     * Productos de un almacén.
     * @param busqueda texto para filtrar por nombre de producto; null o vacío = todos.
     */
    public List<ExistenciaDTO> listarExistencias(Long almacenId, String busqueda) {
        throw new UnsupportedOperationException("Pendiente");
    }

    /**
     * Aplica un movimiento (entrada, salida, ajuste o traslado) y lo guarda en el historial.
     * @throws StockInsuficienteException si se intenta sacar más de lo que hay.
     */
    public void registrarMovimiento(MovimientoRequest request) {
        throw new UnsupportedOperationException("Pendiente");
    }

    /** Últimos movimientos de un almacén, del más reciente al más antiguo. */
    public List<MovimientoDTO> historial(Long almacenId) {
        throw new UnsupportedOperationException("Pendiente");
    }
}
