package com.ceu.inventario.repository;

import com.ceu.inventario.modelo.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository <Producto,Long> {

}
