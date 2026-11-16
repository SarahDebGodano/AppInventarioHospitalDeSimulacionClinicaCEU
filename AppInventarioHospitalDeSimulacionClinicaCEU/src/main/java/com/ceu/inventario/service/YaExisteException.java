package com.ceu.inventario.service;

public class YaExisteException extends RuntimeException{
    public YaExisteException(String mensaje) {
        super(mensaje);
    }
}
