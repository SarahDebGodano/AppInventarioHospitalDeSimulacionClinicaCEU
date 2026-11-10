package com.ceu.inventario.dto;

import java.time.LocalDate;

public record MovimientoDTO(
        Long id,
        Tipomoviento tipo,
        String producto,
        int cantidad,
        String usuario,
        LocalDateTime fecha
) {
}
