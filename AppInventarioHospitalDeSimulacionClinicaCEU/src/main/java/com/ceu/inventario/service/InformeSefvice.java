package com.ceu.inventario.service;

import com.ceu.inventario.dto.ExistenciaDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InformeSefvice {
    /** PDF con el resumen de todos los almacenes. Devuelve los bytes del fichero. */
    public byte[] generarPdfInventario() {
        throw new UnsupportedOperationException("Pendiente");
    }

    /** Productos por debajo del stock mínimo en cualquier almacén. */
    public List<ExistenciaDTO> listarBajoMinimo() {
        throw new UnsupportedOperationException("Pendiente");
    }
}
