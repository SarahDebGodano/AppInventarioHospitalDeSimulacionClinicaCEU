package com.ceu.inventario.repository;

import com.ceu.inventario.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    List<Usuario> findByActivoTrueOrderByNombreAsc();

}
