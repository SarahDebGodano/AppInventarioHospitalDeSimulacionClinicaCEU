// Generador de códigos QR sin dependencias (funciona sin conexión a internet).
// Modo byte, corrección de errores M, versiones 1 a 10 (hasta ~210 caracteres).
// Uso:  QR.svg('http://servidor/almacen.html?id=1', 'Almacén 1')  ->  texto SVG listo para insertar.
const QR = (function () {
    // [bloques [cantidad, bytes de datos]], bytes de corrección por bloque, nº de módulos.
    const VERSIONES = [null,
        { ec: 10, bloques: [[1, 16]] },
        { ec: 16, bloques: [[1, 28]] },
        { ec: 26, bloques: [[1, 44]] },
        { ec: 18, bloques: [[2, 32]] },
        { ec: 24, bloques: [[2, 43]] },
        { ec: 16, bloques: [[4, 27]] },
        { ec: 18, bloques: [[4, 31]] },
        { ec: 22, bloques: [[2, 38], [2, 39]] },
        { ec: 22, bloques: [[3, 36], [2, 37]] },
        { ec: 26, bloques: [[4, 43], [1, 44]] },
    ];
    const ALINEACION = [null, [], [6, 18], [6, 22], [6, 26], [6, 30], [6, 34], [6, 22, 38], [6, 24, 42], [6, 26, 46], [6, 28, 50]];

    const bytesDeDatos = v => VERSIONES[v].bloques.reduce((s, [n, d]) => s + n * d, 0);

    /* ---------- Reed-Solomon sobre GF(256) ---------- */
    function mul(x, y) {
        let z = 0;
        for (let i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }
    function divisorRS(grado) {
        const r = Array(grado).fill(0);
        r[grado - 1] = 1;
        let raiz = 1;
        for (let i = 0; i < grado; i++) {
            for (let j = 0; j < grado; j++) {
                r[j] = mul(r[j], raiz);
                if (j + 1 < grado) r[j] ^= r[j + 1];
            }
            raiz = mul(raiz, 2);
        }
        return r;
    }
    function restoRS(datos, divisor) {
        const r = Array(divisor.length).fill(0);
        for (const b of datos) {
            const factor = b ^ r.shift();
            r.push(0);
            divisor.forEach((c, i) => { r[i] ^= mul(c, factor); });
        }
        return r;
    }

    /* ---------- Penalización para elegir la mejor máscara ---------- */
    function penalizacion(m) {
        const n = m.length;
        let p = 0, oscuros = 0;
        for (let dir = 0; dir < 2; dir++) {
            for (let i = 0; i < n; i++) {
                let racha = 1;
                for (let j = 1; j < n; j++) {
                    const a = dir ? m[j][i] : m[i][j];
                    const b = dir ? m[j - 1][i] : m[i][j - 1];
                    if (a === b) racha++;
                    else { if (racha >= 5) p += racha - 2; racha = 1; }
                }
                if (racha >= 5) p += racha - 2;
                const s = Array.from({ length: n }, (_, j) => ((dir ? m[j][i] : m[i][j]) ? '1' : '0')).join('');
                for (let j = 0; j + 11 <= n; j++) {
                    const t = s.substr(j, 11);
                    if (t === '10111010000' || t === '00001011101') p += 40;
                }
            }
        }
        for (let y = 0; y < n; y++) {
            for (let x = 0; x < n; x++) {
                if (m[y][x]) oscuros++;
                if (x < n - 1 && y < n - 1 && m[y][x] === m[y][x + 1] && m[y][x] === m[y + 1][x] && m[y][x] === m[y + 1][x + 1]) p += 3;
            }
        }
        const total = n * n;
        p += Math.max(0, Math.ceil(Math.abs(oscuros * 20 - total * 10) / total) - 1) * 10;
        return p;
    }

    const condicionMascara = [
        (x, y) => (x + y) % 2 === 0,
        (x, y) => y % 2 === 0,
        (x, y) => x % 3 === 0,
        (x, y) => (x + y) % 3 === 0,
        (x, y) => (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0,
        (x, y) => (x * y) % 2 + (x * y) % 3 === 0,
        (x, y) => ((x * y) % 2 + (x * y) % 3) % 2 === 0,
        (x, y) => ((x + y) % 2 + (x * y) % 3) % 2 === 0,
    ];

    /* ---------- Construcción de la matriz ---------- */
    function matriz(texto) {
        const bytes = Array.from(new TextEncoder().encode(texto));

        let v = 1;
        while (v <= 10 && 4 + (v < 10 ? 8 : 16) + 8 * bytes.length > bytesDeDatos(v) * 8) v++;
        if (v > 10) throw new Error('El texto es demasiado largo para el código QR');

        // 1) Bits de datos: modo byte + longitud + datos + terminador + relleno
        const bits = [];
        const poner = (valor, n) => { for (let i = n - 1; i >= 0; i--) bits.push((valor >>> i) & 1); };
        const capacidad = bytesDeDatos(v) * 8;
        poner(0b0100, 4);
        poner(bytes.length, v < 10 ? 8 : 16);
        bytes.forEach(b => poner(b, 8));
        poner(0, Math.min(4, capacidad - bits.length));
        while (bits.length % 8) bits.push(0);
        for (let relleno = 0xEC; bits.length < capacidad; relleno ^= 0xEC ^ 0x11) poner(relleno, 8);

        const datos = [];
        for (let i = 0; i < bits.length; i += 8) {
            let b = 0;
            for (let j = 0; j < 8; j++) b = (b << 1) | bits[i + j];
            datos.push(b);
        }

        // 2) Bloques con corrección de errores, intercalados
        const info = VERSIONES[v];
        const divisor = divisorRS(info.ec);
        const bloques = [];
        let pos = 0;
        for (const [n, d] of info.bloques) {
            for (let k = 0; k < n; k++) {
                const dat = datos.slice(pos, pos + d);
                pos += d;
                bloques.push({ dat, ec: restoRS(dat, divisor) });
            }
        }
        const palabras = [];
        const maxD = Math.max(...bloques.map(b => b.dat.length));
        for (let i = 0; i < maxD; i++) bloques.forEach(b => { if (i < b.dat.length) palabras.push(b.dat[i]); });
        for (let i = 0; i < info.ec; i++) bloques.forEach(b => palabras.push(b.ec[i]));

        // 3) Patrones fijos
        const n = 17 + 4 * v;
        const mod = Array.from({ length: n }, () => Array(n).fill(false));
        const fijo = Array.from({ length: n }, () => Array(n).fill(false));
        const fijar = (x, y, oscuro) => {
            if (x >= 0 && x < n && y >= 0 && y < n) { mod[y][x] = oscuro; fijo[y][x] = true; }
        };

        for (let i = 0; i < n; i++) { fijar(6, i, i % 2 === 0); fijar(i, 6, i % 2 === 0); }

        const buscador = (cx, cy) => {
            for (let dy = -4; dy <= 4; dy++) for (let dx = -4; dx <= 4; dx++) {
                const d = Math.max(Math.abs(dx), Math.abs(dy));
                fijar(cx + dx, cy + dy, d !== 2 && d !== 4);
            }
        };
        buscador(3, 3); buscador(n - 4, 3); buscador(3, n - 4);

        const pos6 = ALINEACION[v];
        pos6.forEach((cy, i) => pos6.forEach((cx, j) => {
            const esquina = (i === 0 && j === 0) || (i === 0 && j === pos6.length - 1) || (i === pos6.length - 1 && j === 0);
            if (esquina) return;
            for (let dy = -2; dy <= 2; dy++) for (let dx = -2; dx <= 2; dx++) fijar(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
        }));

        const bit = (valor, i) => ((valor >>> i) & 1) !== 0;
        const formato = (escribir, mascara) => {
            const d = (0 << 3) | mascara;               // nivel de corrección M = 00
            let resto = d;
            for (let i = 0; i < 10; i++) resto = (resto << 1) ^ ((resto >>> 9) * 0x537);
            const b = ((d << 10) | resto) ^ 0x5412;
            for (let i = 0; i <= 5; i++) escribir(8, i, bit(b, i));
            escribir(8, 7, bit(b, 6)); escribir(8, 8, bit(b, 7)); escribir(7, 8, bit(b, 8));
            for (let i = 9; i < 15; i++) escribir(14 - i, 8, bit(b, i));
            for (let i = 0; i < 8; i++) escribir(n - 1 - i, 8, bit(b, i));
            for (let i = 8; i < 15; i++) escribir(8, n - 15 + i, bit(b, i));
            escribir(8, n - 8, true);                    // módulo oscuro fijo
        };
        formato(fijar, 0);                             // reserva la zona de formato

        if (v >= 7) {
            let resto = v;
            for (let i = 0; i < 12; i++) resto = (resto << 1) ^ ((resto >>> 11) * 0x1F25);
            const b = (v << 12) | resto;
            for (let i = 0; i < 18; i++) {
                const a = n - 11 + (i % 3), c = Math.floor(i / 3);
                fijar(a, c, bit(b, i)); fijar(c, a, bit(b, i));
            }
        }

        // 4) Colocar los datos en zigzag
        let k = 0;
        for (let derecha = n - 1; derecha >= 1; derecha -= 2) {
            if (derecha === 6) derecha = 5;
            for (let vert = 0; vert < n; vert++) {
                for (let j = 0; j < 2; j++) {
                    const x = derecha - j;
                    const arriba = ((derecha + 1) & 2) === 0;
                    const y = arriba ? n - 1 - vert : vert;
                    if (!fijo[y][x] && k < palabras.length * 8) {
                        mod[y][x] = bit(palabras[k >>> 3], 7 - (k & 7));
                        k++;
                    }
                }
            }
        }

        // 5) Probar las 8 máscaras y quedarse con la de menor penalización
        let mejor = null, mejorPenal = Infinity;
        for (let m = 0; m < 8; m++) {
            const copia = mod.map(f => f.slice());
            for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
                if (!fijo[y][x] && condicionMascara[m](x, y)) copia[y][x] = !copia[y][x];
            }
            formato((x, y, val) => { copia[y][x] = val; }, m);
            const p = penalizacion(copia);
            if (p < mejorPenal) { mejorPenal = p; mejor = copia; }
        }
        return mejor;
    }

    const escapar = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');

    // Devuelve el QR como texto SVG (con zona blanca alrededor, necesaria para que se lea bien).
    function svg(texto, etiqueta) {
        const m = matriz(texto);
        const zona = 4, n = m.length;
        let d = '';
        for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) if (m[y][x]) d += 'M' + (x + zona) + ' ' + (y + zona) + 'h1v1h-1z';
        const lado = n + 2 * zona;
        return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + lado + ' ' + lado + '" shape-rendering="crispEdges" role="img" aria-label="' + escapar(etiqueta || 'Código QR') + '">' +
            '<rect width="100%" height="100%" fill="#fff"/><path d="' + d + '" fill="#000"/></svg>';
    }

    return { matriz, svg };
})();

if (typeof module !== 'undefined') module.exports = QR;