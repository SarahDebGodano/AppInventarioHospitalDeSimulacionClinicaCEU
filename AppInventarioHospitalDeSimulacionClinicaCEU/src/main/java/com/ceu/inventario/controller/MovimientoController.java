package com.ceu.inventario.controller;

import com.ceu.inventario.dto.MovimientoDTO;
import com.ceu.inventario.dto.MovimientoRequest;
import com.ceu.inventario.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final InventarioService inventarioService;

    public MovimientoController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /** POST /api/movimientos: lo que manda el móvil al pulsar + o −. */
    @PostMapping
    public ResponseEntity<Void> registrar(@Valid @RequestBody MovimientoRequest request) {
        inventarioService.registrarMovimiento(request);
        return ResponseEntity.ok().build();
    }

    /** GET /api/movimientos?almacenId=1: historial del almacén, del más reciente al más antiguo. */
    @GetMapping
    public List<MovimientoDTO> historial(@RequestParam("almacenId") Long almacenId) {
        return inventarioService.historial(almacenId);
    }
}