package com.ceu.inventario.dto;

import com.ceu.inventario.modelo.enums.TipoMovimiento;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovimientoDTO(
        Long id,
        TipoMovimiento tipo,
        String producto,
        int cantidad,
        String usuario,
        LocalDateTime fecha
) {
}
