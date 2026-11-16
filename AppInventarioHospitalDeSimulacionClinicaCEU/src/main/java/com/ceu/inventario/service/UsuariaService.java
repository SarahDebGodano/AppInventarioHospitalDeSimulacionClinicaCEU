package com.ceu.inventario.service;

import com.ceu.inventario.dto.UsuarioDTO;
import com.ceu.inventario.modelo.Usuario;
import com.ceu.inventario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuariaService {
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly=true)
    public List<UsuarioDTO> listarUsuarios(){ //¿QUINE HACE CADA MOVIMIENTO?
        List<UsuarioDTO> resultado=new ArrayList<>();
        for(Usuario u: usuarioRepository.findByActivoTrueOrderByNombreAsc()){
            resultado.add(new UsuarioDTO(u.getId(),u.getNombre()));
        }
        return resultado;
    }
}
