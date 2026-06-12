package com.niyotechnologies.claimlens.common.exception;


import com.niyotechnologies.claimlens.common.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.niyotechnologies.claimlens.common.response.ValidationErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ApiErrorResponse.of(
                                ex.getCode(),
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        ApiErrorResponse.of(
                                ex.getCode(),
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ){
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {

                    String fieldName =
                            ((FieldError) error).getField();

                    String errorMessage =
                            error.getDefaultMessage();

                    errors.put(
                            fieldName,
                            errorMessage
                    );
                });

        return ResponseEntity
                .badRequest()
                .body(
                        ValidationErrorResponse.of(errors)
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex
    ) {

        return ResponseEntity
                .internalServerError()
                .body(
                        ApiErrorResponse.of(
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected error occurred"
                        )
                );
    }
}