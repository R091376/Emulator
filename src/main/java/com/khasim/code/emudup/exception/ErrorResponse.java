package com.khasim.code.emudup.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse{
	private HttpStatus status;
	private String message;
	private String debugMessage;
	private LocalDateTime timeStamp;
	
	private ErrorResponse() {
		timeStamp = LocalDateTime.now();
	}

	ErrorResponse(HttpStatus status) {
		this();
		this.status = status;
	}

	ErrorResponse(HttpStatus status, Throwable ex) {
		this();
		this.status = status;
		this.message = "Unexpected error";
		this.debugMessage = ex.getLocalizedMessage();
	}

	ErrorResponse(HttpStatus status, String message, Throwable ex) {
		this();
		this.status = status;
		this.message = message == null || message.equals("") ? "Unexpected error" : message;
		this.debugMessage = ex.getLocalizedMessage();
	}
}