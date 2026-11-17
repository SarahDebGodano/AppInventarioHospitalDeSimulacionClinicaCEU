package com.ceu.inventario.controller;

import com.ceu.inventario.dto.NuevoUsuarioRequest;
import com.ceu.inventario.dto.UsuarioDTO;
import com.ceu.inventario.service.UsuariaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuariaService usuariaService;

    public UsuarioController(UsuariaService usuariaService) {
        this.usuariaService = usuariaService;
    }

    // Mostrar usuarios en orden alfabetico
    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuariaService.listarUsuarios();
    }

    /** POST /api/usuarios {"nombre": "..."}: 201 con el usuario; 409 si ya existe. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO crear(@Valid @RequestBody NuevoUsuarioRequest request) {
        return usuariaService.crearUsuario(request);
    }

    //Elimina al usuaroi pero mantiene el historial
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        usuariaService.eliminarUsuario(id);
    }
}