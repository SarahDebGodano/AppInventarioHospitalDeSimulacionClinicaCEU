package com.ceu.inventario.controller;

import com.ceu.inventario.service.NoEncontradoException;
import com.ceu.inventario.service.StockInsuficienteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Convierte las excepciones en respuestas JSON {"mensaje": "..."}.
 * api.js lee ese campo "mensaje" y lo enseña en la franja morada de la pantalla.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** Se intentó sacar más de lo que hay. */
    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> stockInsuficiente(StockInsuficienteException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** No existe el almacén, el producto o el usuario que pide el movimiento. */
    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoEncontradoException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Datos incorrectos detectados por el service (cantidad 0, falta destino en un traslado...). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argumentoIncorrecto(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Falla alguna validación de MovimientoRequest (@NotNull, @Min, @Size...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosNoValidos(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("datos no válidos");
        return error(HttpStatus.BAD_REQUEST, "Movimiento no válido: " + detalle);
    }

    /** El JSON llegó mal formado o con un valor de tipo incorrecto. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonIncorrecto(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "La petición no tiene el formato esperado.");
    }

    /** TEMPORAL: aún quedan métodos "Pendiente" (UsuariaService.listarUsuarios, InformeSefvice.listarBajoMinimo). */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> pendiente(UnsupportedOperationException e) {
        return error(HttpStatus.NOT_IMPLEMENTED, "Todavía no está implementado en el servidor.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", mensaje));
    }
}