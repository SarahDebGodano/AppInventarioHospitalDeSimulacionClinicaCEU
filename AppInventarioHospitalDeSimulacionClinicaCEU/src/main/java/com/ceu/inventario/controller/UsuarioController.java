package com.ceu.inventario.controller;

import com.ceu.inventario.dto.UsuarioDTO;
import com.ceu.inventario.service.UsuariaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuariaService usuariaService;

    public UsuarioController(UsuariaService usuariaService) {
        this.usuariaService = usuariaService;
    }

    /** GET /api/usuarios */
    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuariaService.listarUsuarios();
    }
}