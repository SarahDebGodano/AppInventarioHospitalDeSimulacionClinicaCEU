package com.ceu.inventario.service;
import com.ceu.inventario.dto.ExistenciaDTO;
import com.ceu.inventario.modelo.Almacen;
import com.ceu.inventario.modelo.Existencia;
import com.ceu.inventario.repository.AlmacenRepository;
import com.ceu.inventario.repository.ExistenciaRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


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
