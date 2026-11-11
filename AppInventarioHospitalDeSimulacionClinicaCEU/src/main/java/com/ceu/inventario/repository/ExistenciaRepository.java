package com.ceu.inventario.repository;

import com.ceu.inventario.modelo.Existencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

//IMPORTNATE: USA LA PANTALLA DEL ALMACEN
public interface ExistenciaRepository extends JpaRepository<Existencia,Long> {
    //BUSCAR TO DO LO QUE HAY EN UN ALMACEN POR NOMBRE DE PRODUCTO
    @Query("""
           SELECT e FROM Existencia e
           JOIN FETCH e.producto p
           LEFT JOIN FETCH p.categoria
           LEFT JOIN FETCH e.ubicacion
           WHERE e.almacen.id = :almacenId
           ORDER BY p.nombre
           """)
    List<Existencia> buscarPorAlmacen(@Param("almacenId") Long almacenId);

    /** Lo que hay en un almacén cuyo nombre contiene un texto (sin importar mayúsculas). */
    @Query("""
           SELECT e FROM Existencia e
           JOIN FETCH e.producto p
           LEFT JOIN FETCH p.categoria
           LEFT JOIN FETCH e.ubicacion
           WHERE e.almacen.id = :almacenId
             AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
           ORDER BY p.nombre
           """)
    List<Existencia> buscarPorAlmacenYNombre(@Param("almacenId") Long almacenId,
                                             @Param("texto") String texto);

    /** La existencia concreta de un producto en un almacén (para sumar o restar). */
    Optional<Existencia> findByAlmacenIdAndProductoId(Long almacenId, Long productoId);

    /** Todo lo que está por debajo de su stock mínimo, en cualquier almacén. */
    @Query("""
           SELECT e FROM Existencia e
           JOIN FETCH e.producto p
           JOIN FETCH e.almacen
           WHERE e.cantidad < e.stockMinimo
           ORDER BY p.nombre
           """)
    List<Existencia> buscarBajoMinimo();
}
