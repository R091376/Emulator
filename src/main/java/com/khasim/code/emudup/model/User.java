package com.khasim.code.emudup.model;

import java.util.List;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class User {
	
	private int id;
	@NotBlank(message = "Usernams is mandatory")
	private String userName;
	private String password;
	private String firstName;
	private String lastName;
	private String email;
	private List<String> forms;
}
