package com.rtm516.mcxboxbroadcast.manager.advice;

import com.rtm516.mcxboxbroadcast.manager.models.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class Advice {
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> endpointNotFound(final NoHandlerFoundException exception) {
        return this.error(HttpStatus.NOT_FOUND, "Endpoint not found.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> resourceNotFound(final NoResourceFoundException exception) {
        return this.error(HttpStatus.NOT_FOUND, "Endpoint not found.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> argumentMismatch(final MethodArgumentTypeMismatchException exception) {
        return this.error(HttpStatus.BAD_REQUEST, "Bad request.");
    }

    private ResponseEntity<ErrorResponse> error(final HttpStatus status, final String error) {
        return new ResponseEntity<>(new ErrorResponse(error), status);
    }
}
