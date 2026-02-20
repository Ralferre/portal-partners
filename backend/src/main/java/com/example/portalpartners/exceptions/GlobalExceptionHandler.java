package com.example.portalpartners.exceptions;

import com.example.portalpartners.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRulesException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRules(
            BusinessRulesException ex, 
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler({ConflictException.class, ResourceAlreadyExists.class})
    public ResponseEntity<ErrorResponse> handleConflict(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "Acesso negado: " + ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "Credenciais inválidas",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        ErrorResponse error = ErrorResponse.of(
                "Erro de validação: " + errors,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String message = "Violação de integridade: dados duplicados ou constraint violation";
        if (ex.getMessage() != null && ex.getMessage().contains("unique")) {
            message = "Registro duplicado. Verifique os dados informados.";
        }
        
        ErrorResponse error = ErrorResponse.of(
                message,
                HttpStatus.CONFLICT.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "Erro interno do servidor: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
