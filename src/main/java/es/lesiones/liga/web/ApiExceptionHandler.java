package es.lesiones.liga.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/** Errores siempre con la forma {"error": "mensaje"} para que el frontend los muestre tal cual. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Las respuestas de error nunca se cachean (el filtro pone caché a los GET públicos antes de saber si fallan). */
    private static ResponseEntity<Map<String, String>> error(HttpStatusCode estado, String mensaje, HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-store");
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> estado(ResponseStatusException ex, HttpServletResponse res) {
        return error(ex.getStatusCode(), String.valueOf(ex.getReason()), res);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException ex, HttpServletResponse res) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Datos no válidos (" + detalle + ")", res);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletResponse res) {
        return error(HttpStatus.BAD_REQUEST, "No se pudo leer la petición (formato de fecha o valor incorrecto)", res);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException ex, HttpServletResponse res) {
        return error(HttpStatus.CONFLICT, "El registro está duplicado o hace referencia a datos que no existen", res);
    }
}
