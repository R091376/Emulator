package com.khasim.code.emudup.model;

import java.util.Set;

import lombok.Data;

@Data
public class DataRequest {
	
	private Set<String> filters;
	private Set<String> sortingOrder;
	private Set<String> groupBy;
	//private Set<String> operations;

}
