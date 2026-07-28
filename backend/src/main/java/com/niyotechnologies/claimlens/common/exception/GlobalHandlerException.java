package com.niyotechnologies.claimlens.common.exception;


import com.niyotechnologies.claimlens.common.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.niyotechnologies.claimlens.common.response.ValidationErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalHandlerException {

    private static final Logger log = LoggerFactory.getLogger(GlobalHandlerException.class);

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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(
            UnauthorizedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
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

    // @PreAuthorize denials surface as AccessDeniedException here (thrown during handler
    // invocation). Without this explicit handler the catch-all below turns them into 500s.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        ApiErrorResponse.of(
                                "FORBIDDEN",
                                "You do not have permission to perform this action"
                        )
                );
    }

    // A non-numeric path variable (e.g. /companies/me hitting /companies/{companyId}) fails to bind
    // and, without this, escapes to the catch-all as a 500 — telling the caller "server broke" when
    // the request was simply malformed. Applies to every {id} route, not just companies.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        ApiErrorResponse.of(
                                "INVALID_PATH_PARAMETER",
                                "'" + ex.getName() + "' must be a valid "
                                        + (ex.getRequiredType() != null
                                                ? ex.getRequiredType().getSimpleName()
                                                : "value")
                        )
                );
    }

    // An unparseable body — malformed JSON, or a value that can't map to the target type such as an
    // unknown enum constant ("decision":"NONSENSE") — reaches Spring as HttpMessageNotReadableException.
    // Without this it escaped to the catch-all as a 500, reporting a server fault for a client-side
    // malformed request. The raw message can expose type/package internals, so it is not echoed back.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        ApiErrorResponse.of(
                                "MALFORMED_REQUEST_BODY",
                                "Request body is missing, malformed, or has an invalid value"
                        )
                );
    }

    // An unmapped URL (a typo'd or renamed endpoint) reaches Spring as NoResourceFoundException.
    // Without this it falls through to the catch-all as a 500 — reporting a server fault for what is
    // simply a wrong path. A missing route is the client's 404, not the server's 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(
            NoResourceFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ApiErrorResponse.of(
                                "RESOURCE_NOT_FOUND",
                                "No endpoint for " + ex.getHttpMethod() + " " + ex.getResourcePath()
                        )
                );
    }

    // A valid path called with an unmapped HTTP verb (e.g. DELETE /claims/1, GET on a PUT-only
    // route) reaches Spring as HttpRequestMethodNotSupportedException. Without this it fell through
    // to the catch-all as a 500 — reporting a server fault for what is a client method error. 405.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(
                        ApiErrorResponse.of(
                                "METHOD_NOT_ALLOWED",
                                ex.getMethod() + " is not supported for this endpoint"
                        )
                );
    }

    // An upload larger than spring.servlet.multipart.max-file-size. Without this it escaped to the
    // catch-all as a 500; the correct answer is 413 so the client knows the file was simply too big.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(
            MaxUploadSizeExceededException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(
                        ApiErrorResponse.of(
                                "FILE_TOO_LARGE",
                                "The uploaded file exceeds the maximum allowed size"
                        )
                );
    }

    // A request with an unsupported Content-Type (e.g. text/plain to a JSON endpoint). Without this
    // it fell through as a 500; the correct answer is 415.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(
                        ApiErrorResponse.of(
                                "UNSUPPORTED_MEDIA_TYPE",
                                "Content-Type '" + ex.getContentType() + "' is not supported"
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex
    ) {

        // The response deliberately reveals nothing (no stack trace, no message) to avoid leaking
        // internals — but the server MUST record it, or a real fault (and any attack that triggers
        // one) leaves no trace. Log server-side only.
        log.error("Unhandled exception", ex);

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