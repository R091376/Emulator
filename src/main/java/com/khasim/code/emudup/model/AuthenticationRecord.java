package com.khasim.code.emudup.model;

import java.util.Date;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AuthenticationRecord {

	private final User user;
	private final String product;
	private final Date expireDate;
}
