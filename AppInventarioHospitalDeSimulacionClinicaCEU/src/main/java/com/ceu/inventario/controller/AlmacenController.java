package com.ceu.inventario.controller;

import com.ceu.inventario.dto.AlmacenDTO;
import com.ceu.inventario.dto.ExistenciaDTO;
import com.ceu.inventario.service.InventarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Puerta de entrada de los almacenes. Sin lógica: recibe, llama al service y devuelve. */
@RestController
@RequestMapping("/api/almacenes")
public class AlmacenController {

    private final InventarioService inventarioService;

    public AlmacenController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /** GET /api/almacenes */
    @GetMapping
    public List<AlmacenDTO> listar() {
        return inventarioService.listarAlmacenes();
    }

    /** GET /api/almacenes/{id}/existencias?busqueda=texto  (busqueda es opcional) */
    @GetMapping("/{id}/existencias")
    public List<ExistenciaDTO> existencias(
            @PathVariable("id") Long id,
            @RequestParam(value = "busqueda", required = false) String busqueda) {
        return inventarioService.listarExistencias(id, busqueda);
    }

    /** DELETE /api/almacenes/{id}/productos/{productoId}?usuarioId=1  -> 204 (quita el producto de ese almacén) */
    @DeleteMapping("/{id}/productos/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarProducto(
            @PathVariable("id") Long id,
            @PathVariable("productoId") Long productoId,
            @RequestParam("usuarioId") Long usuarioId) {
        inventarioService.eliminarDeAlmacen(id, productoId, usuarioId);
    }
}