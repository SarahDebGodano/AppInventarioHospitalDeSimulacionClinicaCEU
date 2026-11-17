package com.ceu.inventario.service;

import com.ceu.inventario.dto.NuevoUsuarioRequest;
import com.ceu.inventario.dto.UsuarioDTO;
import com.ceu.inventario.modelo.Usuario;
import com.ceu.inventario.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuariaService {
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuarios(){ //¿QUINE HACE CADA MOVIMIENTO?
        List<UsuarioDTO> resultado=new ArrayList<>();
        for(Usuario u: usuarioRepository.findByActivoTrueOrderByNombreAsc()){
            resultado.add(new UsuarioDTO(u.getId(),u.getNombre()));
        }
        return resultado;
    }

    //Añadir un usuario
    @Transactional
    public UsuarioDTO crearUsuario(NuevoUsuarioRequest req){
        String nombre = req.nombre() == null ? "" : req.nombre().trim();
        if(nombre.isEmpty()){
            throw new IllegalArgumentException("Por favor escriba su nombre");
        }

        List<Usuario> mismos = usuarioRepository.findAllByNombreIgnoreCase(nombre);
        for (Usuario u : mismos){
            if (u.isActivo()){
                throw new YaExisteException("«" + u.getNombre() + "» ya existe.");
            }
        }
        Usuario usuario;
        if (mismos.isEmpty()) {
            usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario = usuarioRepository.save(usuario);
        } else {
            usuario = mismos.get(0);
            usuario.setActivo(true);   // estaba eliminanado pero vuelve
        }
        return new UsuarioDTO(usuario.getId(), usuario.getNombre());
    }

    //Eliminar ususario
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("El usuario no existe " + id));
        usuario.setActivo(false);
    }

}
