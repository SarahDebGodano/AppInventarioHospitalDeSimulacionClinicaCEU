package com.ceu.inventario.controller;

import com.ceu.inventario.dto.CategoriaDTO;
import com.ceu.inventario.dto.ExistenciaDTO;
import com.ceu.inventario.dto.NuevoProductoRequest;
import com.ceu.inventario.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Productos y categorías. Recibe, llama al service y devuelve. */
@RestController
@RequestMapping("/api")
public class ProductoController {

    private final InventarioService inventarioService;

    public ProductoController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /** GET /api/categorias: para el desplegable de "Añadir producto". */
    @GetMapping("/categorias")
    public List<CategoriaDTO> categorias() {
        return inventarioService.listarCategorias();
    }

    /**
     * POST /api/productos: añade un producto a un almacén (el almacenId va en el cuerpo).
     * 201 con la existencia creada; 409 si ya estaba en ese almacén.
     */
    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ExistenciaDTO crear(@Valid @RequestBody NuevoProductoRequest request) {
        return inventarioService.crearProducto(request);
    }
}