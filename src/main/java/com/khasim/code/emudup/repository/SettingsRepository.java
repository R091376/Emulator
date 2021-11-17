package com.khasim.code.emudup.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.khasim.code.emudup.model.BluForm;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SettingsRepository {
	
	@Value("#{${settings.file.name}}")
	private Map<String, String> formfiles;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private ListMultimap<String, EmulatorForm> formsMap = ArrayListMultimap.create();
	
	@PostConstruct
	public void init() throws IOException {
		loadAllForms();
		log.info("all forms loads");
	}

	private void loadAllForms() throws IOException {
		for(Map.Entry<String, String> map : formfiles.entrySet()) {
			try(InputStream inputStream = new ClassPathResource(map.getValue()).getInputStream()){
				List<String> forms = objectMapper.readValue(inputStream, List.class);
				for(String form : forms) {
					try(InputStream stream = new ClassPathResource(form).getInputStream()){
						EmulatorForm emuForm = objectMapper.readValue(stream, EmulatorForm.class);
						formsMap.put(map.getKey(), emuForm);
					}
				}
			}
		}
	}
	
	public List<BluForm> getAllForms(String productName){
		return formsMap.get(productName).stream().map(form -> form.getForm()).collect(Collectors.toList());
	}
	
	public EmulatorForm getOneForm(String formKey, String productName){
		return formsMap.get(productName).stream()
				.filter(form -> form.getForm().getKey().equalsIgnoreCase(formKey))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No such form exist"));
	}
	
	public List<BluForm> getForms(List<String> formKeys, String productName){
		return formsMap.get(productName).stream()
				.filter(form -> formKeys.contains((form.getForm().getKey())))
				.map(form -> form.getForm())
				.collect(Collectors.toList());
	}
	
	@Data
	public static class EmulatorForm{
		private BluForm form;
		private List<Object[]> data;
	}
}
