package com.taskflowpro.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiError> api(ApiException ex, HttpServletRequest request) {
    return response(ex.getStatus(), ex.getStatus().name(), ex.getMessage(), request, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors())
      fields.putIfAbsent(error.getField(), error.getDefaultMessage());
    return response(
        HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, fields);
  }

  @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
  ResponseEntity<ApiError> stale(Exception ex, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        "STALE_UPDATE",
        "This task changed since you loaded it. Refresh and retry.",
        request,
        null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest request) {
    return response(
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "You do not have permission to perform this action",
        request,
        null);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unknown(Exception ex, HttpServletRequest request) {
    log.error(
        "Unhandled error while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        request,
        null);
  }

  private ResponseEntity<ApiError> response(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      Map<String, String> fields) {
    return ResponseEntity.status(status)
        .body(
            new ApiError(
                Instant.now(), status.value(), code, message, request.getRequestURI(), fields));
  }
}
