// Todas las llamadas al servidor están aquí. El resto de la interfaz solo usa estas funciones.
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
    almacenes: [
        { id: 1, nombre: 'Almacén 1', descripcion: 'Lencería' },
        { id: 8, nombre: 'Pediatría 1', descripcion: 'Armario de Pediatría 1' },
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
};

/* ---------- Utilidad común ---------- */
async function pedir(ruta, opciones) {
    const respuesta = await fetch(API + ruta, opciones);
    if (!respuesta.ok) {
        let mensaje = 'Error ' + respuesta.status;
        try {
            const cuerpo = await respuesta.json();
            mensaje = cuerpo.mensaje || cuerpo.message || mensaje;
        } catch (e) { /* la respuesta no era JSON */ }
        throw new Error(mensaje);
    }
    return respuesta;
}

/* ---------- Funciones públicas ---------- */
// AlmacenDTO: { id, nombre, descripcion }
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

// La pantalla pasa { almacenId, productoId, cantidad, tipo }.
// Aquí se traduce al formato exacto de MovimientoRequest (que usa "productId").
async function postMovimiento(m) {
    if (USAR_MOCK) {
        const fila = (MOCK.existencias[m.almacenId] || []).find(e => e.productoId === m.productoId);
        if (!fila) throw new Error('Producto no encontrado');
        const nueva = fila.cantidad + (m.tipo === 'SALIDA' ? -m.cantidad : m.cantidad);
        if (nueva < 0) throw new Error('No hay suficientes unidades');
        fila.cantidad = nueva;
        fila.bajoMinimo = nueva < fila.stockMinimo;
        return;
    }
    const cuerpo = {
        tipo: m.tipo,
        almacenId: m.almacenId,
        productId: m.productoId,
        cantidad: m.cantidad,
        usuarioId: m.usuarioId ?? USUARIO_POR_DEFECTO,
    };
    await pedir('/movimientos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cuerpo),
    });
}

function descargarInforme() {
    if (USAR_MOCK) { alert('Modo prueba: aquí se descargaría el PDF del inventario.'); return; }
    window.location.href = API + '/informes/inventario.pdf';
}