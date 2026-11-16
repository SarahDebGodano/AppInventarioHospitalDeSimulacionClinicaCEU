package com.ceu.inventario.repository;

import com.ceu.inventario.modelo.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository <Producto,Long> {
    @Query("""
           SELECT p FROM Producto p
           WHERE LOWER(TRIM(p.nombre)) = LOWER(TRIM(:nombre))
           ORDER BY p.id
           """)
    List<Producto> buscarPorNombre(@Param("nombre") String nombre);

}
