package com.ceu.inventario.dto;

public record ExistenciaDTO (
    Long productoId,
    String producto,
    String categoria,
    String ubicacion,
    int cantidad,
    Integer stockMinimo, //integer permite null
    boolean bajoMinimo
    //true: cantidad<stockMinimo para pintar rojo en interfaz

){}
