package com.khasim.code.emudup.model;

import lombok.Data;

@Data
public class RequestToken {

	private String authenticationType;
	private String authCode;
	private String clientId;
	private String clientSecret;
}
