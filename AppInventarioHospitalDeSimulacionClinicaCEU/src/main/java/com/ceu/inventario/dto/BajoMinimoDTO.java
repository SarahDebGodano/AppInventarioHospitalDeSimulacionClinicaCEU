package com.ceu.inventario.dto;

public record BajoMinimoDTO(
        Long almacenId,
        String almacen,
        Long productoId,
        String producto,
        int cantidad,
        int stockMinimo
)
{}
