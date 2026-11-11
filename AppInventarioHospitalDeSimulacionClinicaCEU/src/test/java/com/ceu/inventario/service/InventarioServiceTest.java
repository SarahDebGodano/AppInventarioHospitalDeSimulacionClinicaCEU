package com.ceu.inventario.service;
import com.ceu.inventario.dto.AlmacenDTO;
import com.ceu.inventario.dto.ExistenciaDTO;
import com.ceu.inventario.dto.MovimientoRequest;
import com.ceu.inventario.modelo.enums.TipoMovimiento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba InventarioService contra la base de datos real.
 * @Transactional: al terminar cada test se deshacen los cambios, así que no ensucia los datos.
 */
@SpringBootTest
@Transactional

public class InventarioServiceTest {
    @Autowired
    InventarioService inventarioService;

    /** Busca el id de un almacén por su nombre (para no depender de los números). */
    private Long idDe(String nombreAlmacen) {
        return inventarioService.listarAlmacenes().stream()
                .filter(a -> a.nombre().equals(nombreAlmacen))
                .findFirst().orElseThrow().id();
    }

    /** Busca un producto concreto dentro de un almacén. */
    private ExistenciaDTO buscar(String almacen, String producto) {
        return inventarioService.listarExistencias(idDe(almacen), producto).stream()
                .filter(e -> e.producto().equals(producto))
                .findFirst().orElseThrow();
    }

    @Test
    void hayDoceAlmacenes() {
        List<AlmacenDTO> almacenes = inventarioService.listarAlmacenes();
        assertEquals(12, almacenes.size());
    }

    @Test
    void elAlmacen4Tiene174Productos() {
        assertEquals(174, inventarioService.listarExistencias(idDe("Almacén 4"), null).size());
    }

    @Test
    void elBuscadorEncuentraLosAbbocathDelAlmacen2() {
        List<ExistenciaDTO> resultado = inventarioService.listarExistencias(idDe("Almacén 2"), "abbocath");
        assertEquals(5, resultado.size());
    }

    @Test
    void unaSalidaRestaStock() {
        ExistenciaDTO antes = buscar("Almacén 4", "Gafas nasales");   // 159 en el Excel

        inventarioService.registrarMovimiento(new MovimientoRequest(
                TipoMovimiento.SALIDA, idDe("Almacén 4"), antes.productoId(), 10, 1L, null));

        assertEquals(antes.cantidad() - 10, buscar("Almacén 4", "Gafas nasales").cantidad());
    }

    @Test
    void noSePuedeSacarMasDeLoQueHay() {
        ExistenciaDTO e = buscar("Almacén 4", "Gafas nasales");

        assertThrows(StockInsuficienteException.class, () ->
                inventarioService.registrarMovimiento(new MovimientoRequest(
                        TipoMovimiento.SALIDA, idDe("Almacén 4"), e.productoId(), e.cantidad() + 1, 1L, null)));
    }

    @Test
    void unTrasladoRestaEnOrigenYSumaEnDestino() {
        ExistenciaDTO origenAntes = buscar("Almacén 4", "Gafas nasales");     // 159
        ExistenciaDTO destinoAntes = buscar("Pediatría 1", "Gafas nasales");  // 22

        inventarioService.registrarMovimiento(new MovimientoRequest(
                TipoMovimiento.TRASLADO, idDe("Almacén 4"), origenAntes.productoId(), 5, 1L, idDe("Pediatría 1")));

        assertEquals(origenAntes.cantidad() - 5, buscar("Almacén 4", "Gafas nasales").cantidad());
        assertEquals(destinoAntes.cantidad() + 5, buscar("Pediatría 1", "Gafas nasales").cantidad());
    }
}
