package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.config.SecurityProperties;
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

	private final SecurityProperties securityProperties;

	@Override
	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(securityProperties.getJwtExpirationHours(), ChronoUnit.HOURS);

		return Jwts.builder()
				.subject(user.getUsername())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey())
				.compact();
	}

	@Override
	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	@Override
	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		Date expiration = parseClaims(token).getExpiration();
		return expiration.before(new Date());
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
	}
}