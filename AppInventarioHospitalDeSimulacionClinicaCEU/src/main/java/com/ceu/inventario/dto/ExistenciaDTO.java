package com.ceu.inventario.dto;

public record ExistenciaDTO (
    Long productoId,
    String producto,
    String categoria,
    String ubicacion,
    int cantidad,
    int stockMinimo,
    boolean bajoMinimo
    //true: cantidad<stockMinimo para pintar rojo en interfaz

){}
