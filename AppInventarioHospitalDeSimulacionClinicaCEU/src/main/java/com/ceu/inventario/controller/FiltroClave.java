package com.ceu.inventario.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Comprueba la clave secreta del QR en todas las peticiones a /api/...
 * La clave llega en la cabecera "X-Clave" (api.js la añade sola) o, solo en las
 * peticiones GET, en el parámetro "?clave=" (para el enlace directo del PDF).
 * Si inventario.clave está vacía (vuestros ordenadores), no comprueba nada.
 */
@Component
public class FiltroClave extends OncePerRequestFilter {

    private final byte[] clave;

    public FiltroClave(@Value("${inventario.clave:}") String clave) {
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    /** No filtra si no hay clave configurada, ni las páginas HTML/CSS/JS (solo protege los datos). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return clave.length == 0 || !peticion.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String recibida = peticion.getHeader("X-Clave");
        if (recibida == null && "GET".equals(peticion.getMethod())) {
            recibida = peticion.getParameter("clave");
        }

        // MessageDigest.isEqual compara sin dar pistas por el tiempo que tarda
        if (recibida != null && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            cadena.doFilter(peticion, respuesta);   // clave correcta: sigue al controller
            return;
        }

        respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);   // 401
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write(
                "{\"mensaje\":\"Este dispositivo no está autorizado. Escanea el código QR de cualquier almacén para activarlo.\"}");
    }
}
