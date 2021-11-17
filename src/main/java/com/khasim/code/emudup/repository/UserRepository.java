package com.khasim.code.emudup.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.khasim.code.emudup.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserRepository {

	public Map<String, List<User>> usersMap = new HashMap<>();
	
	@Value("#{${users.file.name}}")
	private Map<String, String> productUsers;
	
	@PostConstruct
	public void init() throws IOException {
		loadUsersFromFiles();
		log.info("User's data loaded");
		
	}

	public void loadUsersFromFiles() throws IOException {
		for(Map.Entry<String, String> products : productUsers.entrySet()) {
			String usersFile = products.getValue();
			String product = products.getKey();
			try(InputStream inputStream = new ClassPathResource(usersFile).getInputStream()){
				try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
					List<User> users = reader.lines().map(line -> addUserToList(line)).collect(Collectors.toList());
					usersMap.put(product, users);
				}
			}
		}
	}

	public User addUserToList(String line) {
		String[] userArr = line.split(";");
		return createUser(Integer.parseInt(userArr[0]), userArr[1], userArr[2], userArr[3], userArr[4], userArr[5], Arrays.asList(userArr[6].split("\\s*,\\s*")));
	}

	public User createUser(Integer id, String userName, String password, String firstName, String lastName,
			String email, List<String> forms) {
		User user = new User();
		user.setId(id);
		user.setUserName(userName);
		user.setPassword(password);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmail(email);
		user.setForms(forms);
		return user;
	}
	
	public List<User> getAllUsers(String productName){
		return usersMap.get(productName);
	}
	
	public User getUser(String productName, String userName) {
		return usersMap.get(productName).stream().filter(user -> userName.equalsIgnoreCase(user.getUserName())).findFirst().orElse(null);
	}
	
	public User findByUserNameAndPasswordFromProduct(String userName, String password, String product) {
		return usersMap.get(product).stream().filter(user -> 
					user.getUserName().equalsIgnoreCase(userName) &&
					user.getPassword().equalsIgnoreCase(password)
					).findFirst()
					.orElse(null);
	}
	
}
