package com.khasim.code.emudup.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

	@ExceptionHandler(value = { IllegalStateException.class, IllegalArgumentException.class,
			NullPointerException.class })
	protected ResponseEntity<Object> handleError(RuntimeException exception) {
		exception.printStackTrace();
		log.error("encountered runtime exception {}", exception.getMessage());
		return buildResponseEntity(
				new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception));
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleGeneralError(Exception exception){
		log.error("encountered exception {}", exception.getMessage());
		exception.printStackTrace();
		return buildResponseEntity(
				new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception));
	}
	
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		return errors;
	}
	
	private ResponseEntity<Object> buildResponseEntity(ErrorResponse errorResponse) {
	       return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
	}
	
}
