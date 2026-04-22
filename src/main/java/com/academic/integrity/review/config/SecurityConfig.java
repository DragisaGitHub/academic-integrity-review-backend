package com.academic.integrity.review.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(Customizer.withDefaults());
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(
						"/api/health",
						"/actuator/health",
						"/actuator/health/**",
						"/api/documents/**",
						"/api/analyses/**",
						"/api/notifications/**",
						"/api/settings/**",
						"/swagger-ui.html",
						"/swagger-ui/**",
						"/v3/api-docs/**"
				).permitAll()
				.anyRequest().authenticated()
		);

		http.httpBasic(Customizer.withDefaults());
		http.formLogin(AbstractHttpConfigurer::disable);

		return http.build();
	}
}
