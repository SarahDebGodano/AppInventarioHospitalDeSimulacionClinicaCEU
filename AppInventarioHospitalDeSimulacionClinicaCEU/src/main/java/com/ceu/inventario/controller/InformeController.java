package com.ceu.inventario.controller;

import com.ceu.inventario.dto.ExistenciaDTO;
import com.ceu.inventario.service.InformeSefvice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/informes")
public class InformeController {

    // InformeSefvice (con f).
    // SI SE RENOMBRA A InformeService, hay que cambiar el import y las dos apariciones de abajo.
    private final InformeSefvice informeService;

    public InformeController(InformeSefvice informeService) {
        this.informeService = informeService;
    }

    /** GET /api/informes/inventario.pdf: el PDF de "Imprimir inventario". */
    @GetMapping("/inventario.pdf")
    public ResponseEntity<byte[]> inventarioPdf() {
        byte[] pdf = informeService.generarPdfInventario();
        // El tipo se pone aquí y no en el @GetMapping (produces=...), para que un error
        // se pueda devolver como JSON sin que Spring responda 406.
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"inventario.pdf\"")
                .body(pdf);
    }

    /** GET /api/informes/bajo-minimo: productos por debajo del mínimo en cualquier almacén. */
    @GetMapping("/bajo-minimo")
    public List<ExistenciaDTO> bajoMinimo() {
        return informeService.listarBajoMinimo();
    }
}