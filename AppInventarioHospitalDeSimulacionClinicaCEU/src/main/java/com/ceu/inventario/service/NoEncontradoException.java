package com.ceu.inventario.service;

public class NoEncontradoException extends RuntimeException{
    public NoEncontradoException(String mensaje){
        super(mensaje);
    }
}
