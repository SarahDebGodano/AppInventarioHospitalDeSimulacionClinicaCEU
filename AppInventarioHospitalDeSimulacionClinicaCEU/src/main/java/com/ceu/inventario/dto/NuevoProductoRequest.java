package com.ceu.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NuevoProductoRequest(
        @NotNull Long almacenId,
        @NotBlank @Size(max = 150) String nombre,
        Long categoriaId,          // opcional: null = sin categoría
        @Min(0) int cantidad,      // unidades iniciales en este almacén
        @Min(0) Integer stockMinimo, //solo cuenta si es fungible
        @NotNull Long usuarioId

) {}
