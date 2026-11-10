package com.ceu.inventario.dto;

import com.ceu.inventario.modelo.enums.TipoMovimiento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimientoRequest (
    @NotNull TipoMovimiento tipo,
    @NotNull Long almacenId,
    @NotNull Long productId,
    @Min(0) int cantidad,
    @NotNull long usuarioId,
    Long almacenDestinoId,
    @Size(max=255)String motivo
    ){}
