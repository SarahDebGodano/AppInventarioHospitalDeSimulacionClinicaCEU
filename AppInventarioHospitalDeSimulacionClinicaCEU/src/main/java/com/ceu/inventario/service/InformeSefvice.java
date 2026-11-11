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
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfShading;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Informes para las encargadas de inventario. Diseño basado en el tema Bootswatch Quartz. */
@Service
@RequiredArgsConstructor

public class InformeSefvice {
    private final AlmacenRepository almacenRepository;
    private final ExistenciaRepository existenciaRepository;

    private static final Color CIAN         = new Color(0x33, 0xB7, 0xE2);
    private static final Color MORADO       = new Color(0x5E, 0x62, 0xB0);
    private static final Color ROSA         = new Color(0xDC, 0x30, 0x7C);
    private static final Color PRIMARIO     = new Color(0xE8, 0x32, 0x83);
    private static final Color PURPURA      = new Color(0x68, 0x6D, 0xC3);
    private static final Color TEXTO        = new Color(0x34, 0x3A, 0x40);
    private static final Color TEXTO_SUAVE  = new Color(0x6C, 0x75, 0x7D);
    private static final Color BORDE        = new Color(0xDE, 0xE2, 0xE6);
    private static final Color FILA_ALTERNA = new Color(0xF4, 0xF4, 0xFB);
    private static final Color FONDO_TOTAL  = new Color(0xFC, 0xE4, 0xEF);
    private static final Color FONDO_ALERTA = new Color(0xFE, 0xE1, 0xE9);


    private static final Font TITULO    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARIO);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PURPURA);
    private static final Font NORMAL    = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXTO);
    private static final Font TOTAL     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARIO);
    private static final Font CABECERA  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font PEQUENA   = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXTO_SUAVE);
    private static final Font BANDA     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

    private static final float MARGEN = 36;

    @Transactional(readOnly = true)
    public byte[] generarPdfInventario() {
        List<Almacen> almacenes = almacenRepository.findAllByOrderByNombreAsc();

        // Se cargan las existencias de cada almacén una sola vez (sirven para el resumen y el detalle)
        List<List<Existencia>> existenciasPorAlmacen = new ArrayList<>();
        for (Almacen a : almacenes) {
            existenciasPorAlmacen.add(existenciaRepository.buscarPorAlmacen(a.getId()));
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        // Margen superior mayor (64) para dejar sitio a la banda de color; inferior (48) para el pie
        Document documento = new Document(PageSize.A4, MARGEN, MARGEN, 64, 48);

        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new CabeceraYPie());
            documento.open();

            escribirPortada(documento, almacenes, existenciasPorAlmacen);

            for (int i = 0; i < almacenes.size(); i++) {
                documento.newPage();
                escribirAlmacen(documento, almacenes.get(i), existenciasPorAlmacen.get(i));
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("No se ha podido generar el PDF", e);
        } finally {
            documento.close();
        }

        return salida.toByteArray();
    }

    /** Productos por debajo del stock mínimo en cualquier almacén. (Pendiente: falta el almacén en el DTO) */
    public List<ExistenciaDTO> listarBajoMinimo() {
        throw new UnsupportedOperationException("Pendiente");
    }
    private void escribirPortada(Document documento, List<Almacen> almacenes,
                                 List<List<Existencia>> existenciasPorAlmacen) {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        documento.add(new Paragraph("Inventario de almacenes", TITULO));
        documento.add(new Paragraph("Hospital de Simulación Clínica CEU · Generado el " + fecha, PEQUENA));

        Paragraph resumenTitulo = new Paragraph("Resumen por almacén", SUBTITULO);
        resumenTitulo.setSpacingBefore(18);
        documento.add(resumenTitulo);

        PdfPTable tabla = new PdfPTable(new float[]{5, 2, 2, 2});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(8);
        tabla.setHeaderRows(1);

        celdaCabecera(tabla, "Almacén");
        celdaCabecera(tabla, "Productos");
        celdaCabecera(tabla, "Unidades");
        celdaCabecera(tabla, "Bajo mínimo");

        int totalProductos = 0;
        int totalUnidades = 0;
        int totalBajoMinimo = 0;

        for (int i = 0; i < almacenes.size(); i++) {
            List<Existencia> existencias = existenciasPorAlmacen.get(i);

            int unidades = 0;
            int bajoMinimo = 0;
            for (Existencia e : existencias) {
                unidades += e.getCantidad();
                if (e.estaBajoMinimo()) {
                    bajoMinimo++;
                }
            }

            Color fondo = null;
            if (i % 2 == 1) {
                fondo = FILA_ALTERNA;
            }

            celda(tabla, nombreCompleto(almacenes.get(i)), NORMAL, Element.ALIGN_LEFT, fondo);
            celda(tabla, String.valueOf(existencias.size()), NORMAL, Element.ALIGN_RIGHT, fondo);
            celda(tabla, String.valueOf(unidades), NORMAL, Element.ALIGN_RIGHT, fondo);
            celda(tabla, String.valueOf(bajoMinimo), NORMAL, Element.ALIGN_RIGHT, fondo);

            totalProductos += existencias.size();
            totalUnidades += unidades;
            totalBajoMinimo += bajoMinimo;
        }

        celda(tabla, "TOTAL", TOTAL, Element.ALIGN_LEFT, FONDO_TOTAL);
        celda(tabla, String.valueOf(totalProductos), TOTAL, Element.ALIGN_RIGHT, FONDO_TOTAL);
        celda(tabla, String.valueOf(totalUnidades), TOTAL, Element.ALIGN_RIGHT, FONDO_TOTAL);
        celda(tabla, String.valueOf(totalBajoMinimo), TOTAL, Element.ALIGN_RIGHT, FONDO_TOTAL);

        documento.add(tabla);
    }

    private void escribirAlmacen(Document documento, Almacen almacen, List<Existencia> existencias) {
        documento.add(new Paragraph(nombreCompleto(almacen), TITULO));

        int unidades = 0;
        for (Existencia e : existencias) {
            unidades += e.getCantidad();
        }
        documento.add(new Paragraph(existencias.size() + " productos · " + unidades + " unidades", PEQUENA));

        if (existencias.isEmpty()) {
            Paragraph vacio = new Paragraph("No hay productos registrados en este almacén.", NORMAL);
            vacio.setSpacingBefore(12);
            documento.add(vacio);
            return;
        }

        PdfPTable tabla = new PdfPTable(new float[]{7, 3, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10);
        tabla.setHeaderRows(1);   // la cabecera se repite si la tabla ocupa varias páginas

        celdaCabecera(tabla, "Producto");
        celdaCabecera(tabla, "Categoría");
        celdaCabecera(tabla, "Cantidad");
        celdaCabecera(tabla, "Mínimo");

        for (int i = 0; i < existencias.size(); i++) {
            Existencia e = existencias.get(i);

            // Prioridad: bajo mínimo (rojo) > fila alterna (lavanda) > blanco
            Color fondo = null;
            if (e.estaBajoMinimo()) {
                fondo = FONDO_ALERTA;
            } else if (i % 2 == 1) {
                fondo = FILA_ALTERNA;
            }

            String categoria = "-";
            if (e.getProducto().getCategoria() != null) {
                categoria = e.getProducto().getCategoria().getNombre();
            }

            celda(tabla, e.getProducto().getNombre(), NORMAL, Element.ALIGN_LEFT, fondo);
            celda(tabla, categoria, NORMAL, Element.ALIGN_LEFT, fondo);
            celda(tabla, String.valueOf(e.getCantidad()), NORMAL, Element.ALIGN_RIGHT, fondo);
            celda(tabla, String.valueOf(e.getStockMinimo()), NORMAL, Element.ALIGN_RIGHT, fondo);
        }

        documento.add(tabla);
    }

    private String nombreCompleto(Almacen a) {
        if (a.getDescripcion() == null || a.getDescripcion().isBlank()) {
            return a.getNombre();
        }
        return a.getNombre() + " · " + a.getDescripcion();
    }

    private void celdaCabecera(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, CABECERA));
        celda.setBackgroundColor(PURPURA);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.5f);
        celda.setPadding(5);
        tabla.addCell(celda);
    }

    private void celda(PdfPTable tabla, String texto, Font fuente, int alineacion, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.5f);
        celda.setPadding(4);
        if (fondo != null) {
            celda.setBackgroundColor(fondo);
        }
        tabla.addCell(celda);
    }

    private static class CabeceraYPie extends PdfPageEventHelper {

        private static final float ALTO_BANDA = 30;
        private static final String RUTA_LOGO = "/static/img/logo-ceu.png";

        private Image logo;   // se carga una sola vez; si no existe el fichero, queda en null

        CabeceraYPie() {
            try {
                URL url = InformeSefvice.class.getResource(RUTA_LOGO);
                if (url != null) {
                    logo = Image.getInstance(url);
                    logo.scaleToFit(90, 20);   // como máximo 90 de ancho y 20 de alto, sin deformarlo
                }
            } catch (Exception e) {
                logo = null;   // si el logo falla, el PDF se genera igualmente, sin logo
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            Rectangle pagina = documento.getPageSize();
            float ancho = pagina.getWidth();
            float alto = pagina.getHeight();
            float mitad = ancho / 2;
            float yBanda = alto - ALTO_BANDA;

            // --- Banda con degradado (por debajo del contenido) ---
            PdfContentByte fondo = writer.getDirectContentUnder();

            // Mitad izquierda: cian -> morado
            fondo.saveState();
            fondo.rectangle(0, yBanda, mitad, ALTO_BANDA);
            fondo.clip();
            fondo.newPath();
            fondo.paintShading(PdfShading.simpleAxial(writer, 0, alto, mitad, alto, CIAN, MORADO));
            fondo.restoreState();

            // Mitad derecha: morado -> rosa
            fondo.saveState();
            fondo.rectangle(mitad, yBanda, mitad, ALTO_BANDA);
            fondo.clip();
            fondo.newPath();
            fondo.paintShading(PdfShading.simpleAxial(writer, mitad, alto, ancho, alto, MORADO, ROSA));
            fondo.restoreState();

            // --- Textos, logo y pie (por encima del contenido) ---
            PdfContentByte encima = writer.getDirectContent();

            ColumnText.showTextAligned(encima, Element.ALIGN_LEFT,
                    new Phrase("INVENTARIO · HOSPITAL DE SIMULACIÓN CLÍNICA CEU", BANDA),
                    MARGEN, alto - 19, 0);

            // Logo en la esquina superior derecha, centrado en vertical dentro de la banda
            if (logo != null) {
                float x = ancho - MARGEN - logo.getScaledWidth();
                float y = yBanda + (ALTO_BANDA - logo.getScaledHeight()) / 2;
                logo.setAbsolutePosition(x, y);
                try {
                    encima.addImage(logo);
                } catch (DocumentException e) {
                    // si no se puede dibujar el logo, se sigue sin él
                }
            }

            // Línea fina sobre el pie de página
            encima.saveState();
            encima.setColorStroke(BORDE);
            encima.setLineWidth(0.5f);
            encima.moveTo(MARGEN, 36);
            encima.lineTo(ancho - MARGEN, 36);
            encima.stroke();
            encima.restoreState();

            // Número de página
            ColumnText.showTextAligned(encima, Element.ALIGN_CENTER,
                    new Phrase("Página " + writer.getPageNumber(), PEQUENA),
                    mitad, 24, 0);
        }
    }




}
