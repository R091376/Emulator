package com.khasim.code.emudup.model;

import java.util.List;

import lombok.Data;

@Data
public class BluForm {

	private String key;
	private String label;
	private List<BluField> schema;
	
	public BluField getField(String name) {
		BluField bluField = schema.stream().filter(field -> field.getFieldName().equals(name)).findFirst().orElse(null);
		return bluField;
	}
	
}
