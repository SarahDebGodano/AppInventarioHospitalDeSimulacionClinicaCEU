package com.ceu.inventario.repository;
import com.ceu.inventario.modelo.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
//INTERFACES QUE DAN ACCESO A LAS TABLAS. Spring genera el SQL
public interface AlmacenRepository extends JpaRepository<Almacen,Long> {
    /** Todos los almacenes ordenados por nombre. */
    List<Almacen> findAllByOrderByNombreAsc();
}
