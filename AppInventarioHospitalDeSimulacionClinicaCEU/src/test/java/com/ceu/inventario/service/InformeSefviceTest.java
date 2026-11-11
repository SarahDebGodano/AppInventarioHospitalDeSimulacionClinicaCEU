package com.ceu.inventario.service;
import com.ceu.inventario.dto.AlmacenDTO;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba la generación del PDF de "Imprimir inventario".
 * Genera el PDF una vez antes de cada test, lo vuelve a abrir con PdfReader
 * y comprueba su contenido leyendo el texto de las páginas.
 */
@SpringBootTest

public class InformeSefviceTest {
    @Autowired
    InformeSefvice informeSefvice;

    @Autowired
    InventarioService inventarioService;

    private byte[] pdf;

    @BeforeEach
    void generarPdf() {
        pdf = informeSefvice.generarPdfInventario();
    }

    /** Lee todo el texto de una página del PDF (la primera página es la 1). */
    private String textoDePagina(PdfReader lector, int pagina) throws Exception {
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        return extractor.getTextFromPage(pagina);
    }

    /** Lee el texto de todas las páginas juntas. */
    private String textoCompleto(PdfReader lector) throws Exception {
        StringBuilder texto = new StringBuilder();
        for (int i = 1; i <= lector.getNumberOfPages(); i++) {
            texto.append(textoDePagina(lector, i)).append("\n");
        }
        return texto.toString();
    }

    // ------------------------------------------------------------------
    // TESTS
    // ------------------------------------------------------------------

    @Test
    void elFicheroEsUnPdfValido() {
        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "El PDF está vacío");

        // Todo fichero PDF empieza por "%PDF"
        String cabecera = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", cabecera);
    }

    @Test
    void tieneAlMenosUnaPaginaPorAlmacenMasLaPortada() throws Exception {
        int numeroAlmacenes = inventarioService.listarAlmacenes().size();

        PdfReader lector = new PdfReader(pdf);
        int paginas = lector.getNumberOfPages();
        lector.close();

        // 1 portada + 1 por almacén. Puede haber más si un almacén ocupa varias páginas (el Almacén 4).
        assertTrue(paginas >= numeroAlmacenes + 1,
                "Se esperaban al menos " + (numeroAlmacenes + 1) + " páginas y hay " + paginas);
    }

    @Test
    void laPortadaTieneElTituloYElTotal() throws Exception {
        PdfReader lector = new PdfReader(pdf);
        String portada = textoDePagina(lector, 1);
        lector.close();

        assertTrue(portada.contains("Inventario de almacenes"), "Falta el título en la portada");
        assertTrue(portada.contains("TOTAL"), "Falta la fila TOTAL en la portada");
    }

    @Test
    void apareceCadaAlmacen() throws Exception {
        PdfReader lector = new PdfReader(pdf);
        String texto = textoCompleto(lector);
        lector.close();

        List<AlmacenDTO> almacenes = inventarioService.listarAlmacenes();
        for (AlmacenDTO almacen : almacenes) {
            assertTrue(texto.contains(almacen.nombre()),
                    "No aparece el almacén " + almacen.nombre() + " en el PDF");
        }
    }

    @Test
    void apareceUnProductoConSuCantidad() throws Exception {
        PdfReader lector = new PdfReader(pdf);
        String texto = textoCompleto(lector);
        lector.close();

        // Del Excel: Abbocath 22 -> 3337 unidades en el Almacén 2
        assertTrue(texto.contains("Abbocath 22"), "No aparece el producto Abbocath 22");
        assertTrue(texto.contains("3337"), "No aparece la cantidad 3337");
    }

    @Test
    void unAlmacenVacioMuestraElAviso() throws Exception {
        PdfReader lector = new PdfReader(pdf);
        String texto = textoCompleto(lector);
        lector.close();

        // El Almacén 7 no tiene productos todavía
        assertTrue(texto.contains("No hay productos registrados"),
                "No aparece el aviso de almacén vacío");
    }

    @Test
    void guardarElPdfParaRevisarloAMano() throws Exception {
        // Se guarda en la carpeta target/, que Git ignora, para abrirlo y revisar el diseño
        Path fichero = Path.of("target", "inventario-prueba.pdf");
        Files.write(fichero, pdf);
        System.out.println("PDF guardado en: " + fichero.toAbsolutePath());

        assertTrue(Files.size(fichero) > 0);
    }

}
