package com.anggaarifandi.holidays.api;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.anggaarifandi.holidays.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(20)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-API-Key";

	private final AppProperties appProperties;
	private final ObjectMapper objectMapper;

	public ApiKeyAuthFilter(AppProperties appProperties, ObjectMapper objectMapper) {
		this.appProperties = appProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return !path.startsWith("/api/v1/");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String apiKey = request.getHeader(HEADER);
		if (apiKey == null || !apiKey.equals(appProperties.apiKey())) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			var body = java.util.Map.of(
					"type", appProperties.error().problemType("unauthorized"),
					"title", "Unauthorized",
					"status", 401,
					"detail", "Missing or invalid X-API-Key header",
					"instance", request.getRequestURI());
			objectMapper.writeValue(response.getOutputStream(), body);
			return;
		}
		filterChain.doFilter(request, response);
	}

}
