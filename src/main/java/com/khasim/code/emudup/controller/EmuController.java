package com.khasim.code.emudup.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;
import com.khasim.code.emudup.model.BluForm;
import com.khasim.code.emudup.model.RequestToken;
import com.khasim.code.emudup.model.User;
import com.khasim.code.emudup.repository.UserRepository;
import com.khasim.code.emudup.security.AuthenticationService;
import com.khasim.code.emudup.service.EmuService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/{productName}")
public class EmuController {
	
	private EmuService emuService;
	private UserRepository userRepository;
	private AuthenticationService authenticationService;
	public final static String loginUrl = "http://localhost:8080";
	
	@PostMapping("/login")
	public ModelAndView login(@PathVariable String productName, @Valid @RequestBody User user) {
		User userObj = userRepository.findByUserNameAndPasswordFromProduct(user.getUserName(), user.getPassword(), productName);
		String redirectUrl = "redirect:"+loginUrl+"/";
		if(userObj == null) {
			redirectUrl = redirectUrl+"/loginFailure";
			return new ModelAndView(redirectUrl, HttpStatus.UNAUTHORIZED);
		}else {
			String authToken = authenticationService.generateInitialAuthToken(userObj, productName);
			redirectUrl = redirectUrl+"/loginSuccess?code="+authToken;
			return new ModelAndView(redirectUrl, HttpStatus.FOUND);
		}
	}
	
	@PostMapping("/token")
	public String getToken(@PathVariable String productName, @RequestBody RequestToken token) {
		return authenticationService.validateAndGenAuthToken(productName, token);
	}
	
	@GetMapping("/user/{userName}")
	public User getUserInfo(@PathVariable String productName, @PathVariable String userName,
			@RequestHeader(value=HttpHeaders.AUTHORIZATION, required = true) String authorizationToken) {
		authenticationService.validateAuthToken(authorizationToken, productName);
		return emuService.getUserInfo(productName, userName);
	}
	
	@GetMapping("/forms")
	public List<BluForm> findAllForms(@PathVariable String productName,
			@RequestHeader(value=HttpHeaders.AUTHORIZATION, required = true) String authorizationToken) {
		User user = authenticationService.validateAuthToken(authorizationToken, productName);
		return emuService.getForms(user.getForms(), productName);
	}
	
	@GetMapping("/{form}/data")
	public List<Map<String, Object>> getData(@PathVariable String productName, @PathVariable String form,
			@RequestHeader(value=HttpHeaders.AUTHORIZATION, required = true) String authorizationToken,
			@RequestParam(required = false) Integer limitBy,
			@RequestParam(required = false) Set<String> filterBy,
			@RequestParam(required = false) Set<String> groupBy,
			@RequestParam(required = false) Set<String> orderBy,
			@RequestParam(required = false) String operation) {
		
		User user = authenticationService.validateAuthToken(authorizationToken, productName);
		Preconditions.checkState(user.getForms().contains(form), "User {} doesn't have access to the form {}", user.getUserName(), form);
		return emuService.getData(productName, form, limitBy, filterBy, groupBy, orderBy, operation);
	}
	
}
