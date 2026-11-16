package com.ceu.inventario.service;

import com.ceu.inventario.dto.*;
import com.ceu.inventario.modelo.*;
import com.ceu.inventario.modelo.enums.TipoMovimiento;
import com.ceu.inventario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service// Spring sabe que esta clase contiene la logica de la app y crea un objeto unico
// al arrancar
@RequiredArgsConstructor// lo inyecta en los repositorios y nunca hacae falta que que se cree un new...
public class InventarioService {

    private final AlmacenRepository almacenRepository;
    private final ProductRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExistenciaRepository existenciaRepository;
    private final MovimientoRepository movimientoRepository;
    private final CategoriaRepository categoriaRepository;

    //Orden de los almacenes por nombre
    //Pantalla incial con la lista de almacenes
    //convierte cada almacen en un AlmacenDTO con nombre id y descripciom
    @Transactional(readOnly = true)
    public List<AlmacenDTO> listarAlmacenes() {
        List<AlmacenDTO> resultado = new ArrayList<>();
        for (Almacen a : almacenRepository.findAllByOrderByNombreAsc()) {
            resultado.add(new AlmacenDTO(a.getId(), a.getNombre(), a.getDescripcion(), a.getCodigoQr()));
        }
        return resultado;
    }


    //Productos que hay en un almacen
    //El parametro de entrada "busqueda" es para filtrar por nombre del producto (null o vacio=todos)
    //Pantalla que se abre al escanear el QR que muestra todos los productos en ese almacen con buscador
    //Convierte cada existencai en ExistenciaDTO
    @Transactional(readOnly = true)
    public List<ExistenciaDTO> listarExistencias(Long almacenId, String busqueda) {
        if (!almacenRepository.existsById(almacenId)) {
            throw new NoEncontradoException("No existe el almacén " + almacenId);
        }

        List<Existencia> existencias;
        if (busqueda == null || busqueda.isBlank()) {
            existencias = existenciaRepository.buscarPorAlmacen(almacenId);
        } else {
            existencias = existenciaRepository.buscarPorAlmacenYNombre(almacenId, busqueda.trim());
        }

        List<ExistenciaDTO> resultado = new ArrayList<>();
        for (Existencia e : existencias) {
            resultado.add(aDTO(e));
        }
        return resultado;
    }

    //Ultimos movimientos de un almacen del mas reciente al mas antiguo
    //Quien saco y metio de un almacen
    //cONVEIRTE un movimiento en MovimietnoDTO
    @Transactional(readOnly = true)
    public List<MovimientoDTO> historial(Long almacenId) {
        if (!almacenRepository.existsById(almacenId)) {
            throw new NoEncontradoException("No existe el almacén " + almacenId);
        }

        List<MovimientoDTO> resultado = new ArrayList<>();
        for (Movimiento m : movimientoRepository.findTop100ByAlmacenIdOrderByFechaDesc(almacenId)) {
            resultado.add(new MovimientoDTO(
                    m.getId(),
                    m.getTipo(),
                    m.getProducto().getNombre(),
                    m.getCantidad(),
                    m.getUsuario().getNombre(),
                    m.getFecha()));
        }
        return resultado;
    }

    //MOVIMIENTOS: SUMA, RESTA, TRASLADO, AJUSTE
    //APLICA UN MOVIMIENTO Y LO GUARDA EN EL HISTORIAL

    //ENTRADA= SUMA DE LA CANTIDAD
    //SALIDA= RESTA DE LA CANTIDAD
    //AJUSTES= FIJA LA CANTIDAD NUEVA TRAS UN RECUENTO
    //TRASLADO= RESTA EN EL ALMACEN DE ORIGEN Y SUMA EN EL DE DESTINO

    //Cargar y comporbar datos
    @Transactional   // o se guarda todo (stock + historial) o no se guarda nada
    public void registrarMovimiento(MovimientoRequest req) {
        Almacen almacen = buscarAlmacen(req.almacenId());
        Producto producto = buscarProducto(req.productId());
        Usuario usuario = buscarUsuario(req.usuarioId());

        int cantidad = req.cantidad();
        if (req.tipo() != TipoMovimiento.AJUSTE && cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }

        Movimiento movimiento = new Movimiento();
        movimiento.setTipo(req.tipo());
        movimiento.setAlmacen(almacen);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setCantidad(cantidad);

        switch (req.tipo()) {
            case ENTRADA:
                obtenerOCrear(almacen, producto).sumar(cantidad);
                break;

            case SALIDA:
                buscarExistenciaConStock(almacen, producto).restar(cantidad);
                break;

            case AJUSTE:
                obtenerOCrear(almacen, producto).setCantidad(cantidad);
                break;

            case TRASLADO:
                if (req.almacenDestinoId() == null) {
                    throw new IllegalArgumentException("Falta el almacén de destino");
                }
                if (req.almacenDestinoId().equals(almacen.getId())) {
                    throw new IllegalArgumentException("El almacén de destino es el mismo que el de origen");
                }
                Almacen destino = buscarAlmacen(req.almacenDestinoId());

                buscarExistenciaConStock(almacen, producto).restar(cantidad);
                obtenerOCrear(destino, producto).sumar(cantidad);

                movimiento.setAlmacenDestino(destino);
                break;
        }

        movimientoRepository.save(movimiento);
        // No hace falta llamar a save() en las existencias: al ser entidades cargadas dentro de
        // la transacción, JPA guarda sus cambios automáticamente al terminar el método.
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarCategorias() {
        List<CategoriaDTO> resultado = new ArrayList<>();
        for (Categoria c : categoriaRepository.findAllByOrderByNombreAsc()) {
            resultado.add(new CategoriaDTO(c.getId(), c.getNombre()));
        }
        return resultado;
    }
    //AÑADIR ARTICULO A UN ALMACEN
//1. Si el producto ya existe (mismo nombre) se reutiliza; si no, se crea.
//2. Si ya está en ese almacén -> error (para sumar unidades está el botón +).
//3. Se crea la existencia con su cantidad y su stock mínimo.
//4. Si entra con unidades, se apunta una ENTRADA en el historial.
    @Transactional
    public ExistenciaDTO crearProducto(NuevoProductoRequest req) {
        Almacen almacen = buscarAlmacen(req.almacenId());
        Usuario usuario = buscarUsuario(req.usuarioId());

        String nombre = req.nombre().trim();
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }

        Categoria categoria = null;
        if (req.categoriaId() != null) {
            categoria = categoriaRepository.findById(req.categoriaId())
                    .orElseThrow(() -> new NoEncontradoException("No existe la categoría " + req.categoriaId()));
        }

        // 1. Reutilizar el producto si ya existe con ese nombre
        Producto producto;
        List<Producto> mismos = productoRepository.buscarPorNombre(nombre);
        if (mismos.isEmpty()) {
            producto = new Producto();
            producto.setNombre(nombre);
            producto.setCategoria(categoria);
            producto = productoRepository.save(producto);
        } else {
            producto = mismos.get(0);
            producto.setActivo(true);   // por si se había eliminado de todos los almacenes
            if (producto.getCategoria() == null && categoria != null) {
                producto.setCategoria(categoria);
            }
        }

        // 2. No duplicar dentro del mismo almacén
        if (existenciaRepository.findByAlmacenIdAndProductoId(almacen.getId(), producto.getId()).isPresent()) {
            throw new YaExisteException("«" + producto.getNombre() + "» ya está en " + almacen.getNombre()
                    + ". Búscalo y usa + para añadir unidades.");
        }

        // 3. Crear la existencia
        Existencia existencia = new Existencia();
        existencia.setAlmacen(almacen);
        existencia.setProducto(producto);
        existencia.setCantidad(req.cantidad());
        existencia.setStockMinimo(req.stockMinimo());
        existencia = existenciaRepository.save(existencia);

        // 4. Dejar constancia en el historial
        if (req.cantidad() > 0) {
            Movimiento movimiento = new Movimiento();
            movimiento.setTipo(TipoMovimiento.ENTRADA);
            movimiento.setAlmacen(almacen);
            movimiento.setProducto(producto);
            movimiento.setUsuario(usuario);
            movimiento.setCantidad(req.cantidad());
            movimientoRepository.save(movimiento);
        }

        return aDTO(existencia);
    }



    //utilis:
    private Almacen buscarAlmacen(Long id) {
        Optional<Almacen> encontrado = almacenRepository.findById(id);
        if (encontrado.isEmpty()) {
            throw new NoEncontradoException("No existe el almacén " + id);
        }
        return encontrado.get();
    }
    private Producto buscarProducto(Long id) {
        Optional<Producto> encontrado = productoRepository.findById(id);
        if (encontrado.isEmpty()) {
            throw new NoEncontradoException("No existe el producto " + id);
        }
        return encontrado.get();
    }

    private Usuario buscarUsuario(Long id) {
        Optional<Usuario> encontrado = usuarioRepository.findById(id);
        if (encontrado.isEmpty()) {
            throw new NoEncontradoException("No existe el usuario " + id);
        }
        return encontrado.get();
    }
    private Existencia buscarExistenciaConStock(Almacen almacen, Producto producto) {
        Optional<Existencia> encontrada =
                existenciaRepository.findByAlmacenIdAndProductoId(almacen.getId(), producto.getId());
        if (encontrada.isEmpty()) {
            throw new StockInsuficienteException(
                    "No hay " + producto.getNombre() + " en " + almacen.getNombre());
        }
        return encontrada.get();
    }
    private Existencia obtenerOCrear(Almacen almacen, Producto producto) {
        Optional<Existencia> encontrada =
                existenciaRepository.findByAlmacenIdAndProductoId(almacen.getId(), producto.getId());
        if (encontrada.isPresent()) {
            return encontrada.get();
        }

        Existencia nueva = new Existencia();
        nueva.setAlmacen(almacen);
        nueva.setProducto(producto);
        nueva.setCantidad(0);
        return existenciaRepository.save(nueva);
    }
    private ExistenciaDTO aDTO(Existencia e) {
        Producto p = e.getProducto();

        String categoria = null;
        if (p.getCategoria() != null) {
            categoria = p.getCategoria().getNombre();
        }

        String ubicacion = null;
        if (e.getUbicacion() != null) {
            ubicacion = e.getUbicacion().getNombre();
        }

        return new ExistenciaDTO(
                p.getId(),
                p.getNombre(),
                categoria,
                ubicacion,
                e.getCantidad(),
                e.getStockMinimo(),
                e.estaBajoMinimo());
    }
}
