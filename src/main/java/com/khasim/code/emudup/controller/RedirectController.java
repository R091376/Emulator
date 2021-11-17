package com.khasim.code.emudup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {
	
	@GetMapping("/loginSuccess")
	public String loginSuccess(@RequestParam(value = "code") String code) {
		return code;
	}
	
	@GetMapping("/loginFailure")
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public String loginFailure() {
		return "Login Failure";
	}
}
