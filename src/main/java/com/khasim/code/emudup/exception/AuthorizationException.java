package com.khasim.code.emudup.exception;

public class AuthorizationException extends RuntimeException{
	
	private String exceptionMessage;
	
	public AuthorizationException(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}
}
