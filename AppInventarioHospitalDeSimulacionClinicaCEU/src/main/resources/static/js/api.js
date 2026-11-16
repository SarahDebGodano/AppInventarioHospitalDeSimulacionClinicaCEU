// Todas las llamadas al servidor. El resto de la interfaz solo usa estas funciones.
// USAR_MOCK = true  -> datos falsos en el navegador (sin servidor).
// USAR_MOCK = false -> habla con la API de Spring (/api/...).
const USAR_MOCK = false;
const API = '/api';

// La tabla movimiento exige un usuario (usuario_id NOT NULL) y ya no hay selector.
// Mientras no se decida otra cosa, todos los movimientos se guardan con este usuario.
// 1 = "Usuario de prueba 1" (V2__datos_iniciales.sql).
const USUARIO_POR_DEFECTO = 1;

/* ---------- Datos falsos (solo con USAR_MOCK = true) ---------- */
const MOCK = {
    usuarios: [{ id: 1, nombre: 'Usuario de prueba 1' }],
    categorias: [
        { id: 1, nombre: 'Fungible' },
        { id: 2, nombre: 'Simuladores' },
        { id: 3, nombre: 'Electromedicina' },
    ],
    almacenes: [
        { id: 1, nombre: 'Almacén 1', descripcion: 'Lencería', codigoQr: 'ALM-01' },
        { id: 8, nombre: 'Pediatría 1', descripcion: 'Armario de Pediatría 1', codigoQr: 'PED-01' },
    ],
    existencias: {
        1: [
            { productoId: 10, producto: 'Almohadas', categoria: null, ubicacion: null, cantidad: 4, stockMinimo: 0, bajoMinimo: false },
            { productoId: 11, producto: 'Batas desechables (paquetes)', categoria: 'Fungible', ubicacion: null, cantidad: 3, stockMinimo: 5, bajoMinimo: true },
        ],
        8: [
            { productoId: 12, producto: 'Cánula nasal O2', categoria: 'Fungible', ubicacion: null, cantidad: 11, stockMinimo: 0, bajoMinimo: false },
        ],
    },
    historial: {},          // almacenId -> lista de MovimientoDTO (el más reciente primero)
    siguienteId: 100,       // para productos y movimientos nuevos
};

function mockFila(almacenId, productoId) {
    return (MOCK.existencias[almacenId] || []).find(e => e.productoId === productoId);
}

function mockNombreAlmacen(id) {
    const a = MOCK.almacenes.find(x => x.id === id);
    return a ? a.nombre : null;
}

// Apunta un movimiento en el historial falso del almacén (como haría el servidor)
function mockAnotar(almacenId, tipo, producto, cantidad, destinoId) {
    if (!MOCK.historial[almacenId]) MOCK.historial[almacenId] = [];
    MOCK.historial[almacenId].unshift({
        id: ++MOCK.siguienteId,
        tipo: tipo,
        producto: producto,
        cantidad: cantidad,
        usuario: MOCK.usuarios[0].nombre,
        fecha: new Date().toISOString(),
        almacenDestino: destinoId ? mockNombreAlmacen(destinoId) : null,
    });
}

function mockActualizar(fila, cantidad) {
    fila.cantidad = cantidad;
    fila.bajoMinimo = cantidad < fila.stockMinimo;
}

/* ---------- Utilidad común ---------- */
// Si el servidor responde con error, lanza un Error con el "mensaje" del servidor.
// error.estado guarda el código HTTP (409 = ya existe, 404 = no existe, ...).
async function pedir(ruta, opciones) {
    const respuesta = await fetch(API + ruta, opciones);
    if (!respuesta.ok) {
        let mensaje = 'Error ' + respuesta.status;
        try {
            const cuerpo = await respuesta.json();
            mensaje = cuerpo.mensaje || cuerpo.message || mensaje;
        } catch (e) { /* la respuesta no era JSON */ }
        const error = new Error(mensaje);
        error.estado = respuesta.status;
        throw error;
    }
    return respuesta;
}

function errorMock(mensaje, estado) {
    const error = new Error(mensaje);
    error.estado = estado;
    return error;
}

/* ---------- Funciones públicas ---------- */
// AlmacenDTO: { id, nombre, descripcion, codigoQr }
async function getAlmacenes() {
    if (USAR_MOCK) return MOCK.almacenes;
    return (await pedir('/almacenes')).json();
}

// ExistenciaDTO: { productoId, producto, categoria, ubicacion, cantidad, stockMinimo, bajoMinimo }
async function getExistencias(almacenId) {
    if (USAR_MOCK) return MOCK.existencias[almacenId] || [];
    return (await pedir('/almacenes/' + almacenId + '/existencias')).json();
}

// UsuarioDTO: { id, nombre }  (ahora mismo la interfaz no lo usa)
async function getUsuarios() {
    if (USAR_MOCK) return MOCK.usuarios;
    return (await pedir('/usuarios')).json();
}

// CategoriaDTO: { id, nombre }   -> GET /api/categorias (para el desplegable de "Añadir producto")
async function getCategorias() {
    if (USAR_MOCK) return MOCK.categorias;
    return (await pedir('/categorias')).json();
}

// MovimientoDTO: { id, tipo, producto, cantidad, usuario, fecha }   -> GET /api/movimientos?almacenId=
// (si el servidor añade "almacenDestino" en los traslados, la pantalla de historial lo enseña)
async function getHistorial(almacenId) {
    if (USAR_MOCK) return MOCK.historial[almacenId] || [];
    return (await pedir('/movimientos?almacenId=' + encodeURIComponent(almacenId))).json();
}

// La pantalla pasa { almacenId, productoId, cantidad, tipo, almacenDestinoId }.
//   ENTRADA / SALIDA -> suma / resta "cantidad"
//   AJUSTE           -> "cantidad" es la cantidad real tras un recuento
//   TRASLADO         -> resta en almacenId y suma en almacenDestinoId
// Aquí se traduce al formato exacto de MovimientoRequest (que usa "productId").
async function postMovimiento(m) {
    if (USAR_MOCK) {
        const fila = mockFila(m.almacenId, m.productoId);
        if (!fila) throw errorMock('Producto no encontrado', 404);

        if (m.tipo === 'AJUSTE') {
            if (m.cantidad < 0) throw errorMock('La cantidad no puede ser negativa', 400);
            mockActualizar(fila, m.cantidad);
        } else {
            if (m.cantidad <= 0) throw errorMock('La cantidad debe ser mayor que 0', 400);
            const nueva = fila.cantidad + (m.tipo === 'ENTRADA' ? m.cantidad : -m.cantidad);
            if (nueva < 0) throw errorMock('Solo quedan ' + fila.cantidad + ' unidades de ' + fila.producto, 400);
            mockActualizar(fila, nueva);

            if (m.tipo === 'TRASLADO') {
                if (!m.almacenDestinoId) throw errorMock('Falta el almacén de destino', 400);
                if (m.almacenDestinoId === m.almacenId) throw errorMock('El almacén de destino es el mismo que el de origen', 400);
                if (!MOCK.existencias[m.almacenDestinoId]) MOCK.existencias[m.almacenDestinoId] = [];
                let destino = mockFila(m.almacenDestinoId, m.productoId);
                if (!destino) {
                    destino = { productoId: fila.productoId, producto: fila.producto, categoria: fila.categoria,
                        ubicacion: null, cantidad: 0, stockMinimo: 0, bajoMinimo: false };
                    MOCK.existencias[m.almacenDestinoId].push(destino);
                }
                mockActualizar(destino, destino.cantidad + m.cantidad);
            }
        }
        mockAnotar(m.almacenId, m.tipo, fila.producto, m.cantidad, m.almacenDestinoId);
        return;
    }
    const cuerpo = {
        tipo: m.tipo,
        almacenId: m.almacenId,
        productId: m.productoId,
        cantidad: m.cantidad,
        usuarioId: m.usuarioId ?? USUARIO_POR_DEFECTO,
        almacenDestinoId: m.almacenDestinoId ?? null,   // solo se usa en TRASLADO
    };
    await pedir('/movimientos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cuerpo),
    });
}

// Añade un producto entero a un almacén.
// p = { nombre, categoriaId (o null), cantidad, stockMinimo }
// Devuelve la ExistenciaDTO creada. Si ya está en ese almacén el servidor responde 409
// (error.estado === 409) con el texto en "mensaje".
//   -> POST /api/almacenes/{almacenId}/productos
async function crearProducto(almacenId, p) {
    if (USAR_MOCK) {
        const lista = MOCK.existencias[almacenId] || (MOCK.existencias[almacenId] = []);
        const nombre = p.nombre.trim();
        if (lista.some(e => e.producto.trim().toLowerCase() === nombre.toLowerCase())) {
            throw errorMock('«' + nombre + '» ya está en ' + mockNombreAlmacen(almacenId), 409);
        }
        const cat = MOCK.categorias.find(c => c.id === p.categoriaId);
        const fila = {
            productoId: ++MOCK.siguienteId,
            producto: nombre,
            categoria: cat ? cat.nombre : null,
            ubicacion: null,
            cantidad: p.cantidad,
            stockMinimo: p.stockMinimo,
            bajoMinimo: p.cantidad < p.stockMinimo,
        };
        lista.push(fila);
        if (p.cantidad > 0) mockAnotar(almacenId, 'ENTRADA', nombre, p.cantidad, null);
        return fila;
    }
    const cuerpo = {
        nombre: p.nombre,
        categoriaId: p.categoriaId ?? null,
        cantidad: p.cantidad,
        stockMinimo: p.stockMinimo,
        usuarioId: p.usuarioId ?? USUARIO_POR_DEFECTO,
    };
    const respuesta = await pedir('/almacenes/' + almacenId + '/productos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cuerpo),
    });
    return respuesta.json();
}

// Quita un producto entero de un almacén (se pierden sus unidades en ese almacén).
//   -> DELETE /api/almacenes/{almacenId}/productos/{productoId}   (204 sin cuerpo)
async function eliminarExistencia(almacenId, productoId) {
    if (USAR_MOCK) {
        const lista = MOCK.existencias[almacenId] || [];
        const i = lista.findIndex(e => e.productoId === productoId);
        if (i < 0) throw errorMock('El producto no está en el almacén', 404);
        lista.splice(i, 1);
        return;
    }
    await pedir('/almacenes/' + almacenId + '/productos/' + productoId, { method: 'DELETE' });
}

function descargarInforme() {
    if (USAR_MOCK) { alert('Modo prueba'); return; }
    window.location.href = API + '/informes/inventario.pdf';
}
