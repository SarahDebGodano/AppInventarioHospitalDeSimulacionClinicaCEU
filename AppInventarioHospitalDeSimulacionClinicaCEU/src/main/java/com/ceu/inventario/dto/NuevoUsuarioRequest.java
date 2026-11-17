package com.ceu.inventario.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NuevoUsuarioRequest (
        @NotBlank @Size (max = 100) String nombre
) {}
