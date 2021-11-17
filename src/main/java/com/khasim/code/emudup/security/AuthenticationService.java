package com.khasim.code.emudup.security;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.khasim.code.emudup.exception.AuthorizationException;
import com.khasim.code.emudup.model.AuthenticationRecord;
import com.khasim.code.emudup.model.RequestToken;
import com.khasim.code.emudup.model.User;
import com.khasim.code.emudup.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
	
	public final static String CLIENT_ID = "EmuApp";
	public final static String CLIENT_SECRET = "EmalutorApplicationSecretKey";
	public final static String SECRET_KEY = "EmulatorTokenSecretKey";
	public static long PER_TOKEN_EXPIRE_MILLI = 1000*60*60; 
	public static long TOKEN_EXPIRE_MILLI = 1000*60*60;  // 1 hour
	
	private final UserRepository userRepository;
	
	private final Map<String, AuthenticationRecord> authCodes = new ConcurrentHashMap<>();
	private final Map<String, AuthenticationRecord> activeUsers = new ConcurrentHashMap<>();
	
	public String generateInitialAuthToken(User user, String productName) {
		log.info("generating initial token begin");
		SignatureAlgorithm algorithm = SignatureAlgorithm.HS256; 
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(SECRET_KEY);
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, algorithm.getJcaName());
		Date now = new Date();
		Date expire =  new Date(now.getTime()+TOKEN_EXPIRE_MILLI);
		String code = Jwts.builder()
				.setIssuedAt(now)
				.setSubject(user.getEmail())
				.setIssuer(productName)
				.signWith(algorithm, signingKey)
				.setExpiration(expire)
				.compact();
		authCodes.put(code, new AuthenticationRecord(user, productName, expire));
		return code;
	}
	
	public String validateAndGenAuthToken(String productName, RequestToken token) {
		log.info("validating initial token and gen new token begin");
		Jws<Claims> claims = Jwts.parser()
				.setSigningKey(DatatypeConverter.parseBase64Binary(SECRET_KEY))
				.parseClaimsJws(token.getAuthCode());
		Claims claim = claims.getBody();
		AuthenticationRecord record = authCodes.remove(token.getAuthCode());
		Preconditions.checkState(record != null && claim.getExpiration().getTime() > (new Date()).getTime(), "Auth code not found or expired");
		Preconditions.checkState(claim.getIssuer().equalsIgnoreCase(record.getProduct()), "Invalid Issuer");
		Preconditions.checkState(CLIENT_ID.equalsIgnoreCase(token.getClientId()), "Invalid Client Id");
		Preconditions.checkState(CLIENT_SECRET.equalsIgnoreCase(token.getClientSecret()), "Invalid Client Secret");
	
		SignatureAlgorithm algorithm = SignatureAlgorithm.HS256; 
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(SECRET_KEY);
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, algorithm.getJcaName());
		Date now = new Date();
		Date expire =  new Date(now.getTime()+PER_TOKEN_EXPIRE_MILLI);
		String code = Jwts.builder()
				.setIssuedAt(now)
				.setIssuer(productName)
				.setSubject(record.getUser().getUserName())
				.signWith(algorithm, signingKey)
				.setExpiration(expire)
				.compact();
		activeUsers.put(code, new AuthenticationRecord(record.getUser(), productName, expire));
		return code;
	}

	public User validateAuthToken(String authorizationToken, String productName) {
		log.info("token validation begin");
		Preconditions.checkNotNull(authorizationToken, "Auth token is null");
		Jws<Claims> cliams = Jwts.parser()
				.setSigningKey(DatatypeConverter.parseBase64Binary(SECRET_KEY))
				.parseClaimsJws(authorizationToken);
		Claims claim = cliams.getBody();
		if(claim.getExpiration().getTime() < (new Date()).getTime()) {
			activeUsers.remove(authorizationToken);
			throw new AuthorizationException("auth token has expired");
		}
		AuthenticationRecord record = activeUsers.get(authorizationToken);
		Preconditions.checkNotNull(record, "auth token is not found");
		Preconditions.checkState(claim.getIssuer().equalsIgnoreCase(productName), "Invalid Issuer");
		Preconditions.checkNotNull(userRepository.getUser(productName, claim.getSubject()), "User not found");
		return record.getUser();
	}

}
