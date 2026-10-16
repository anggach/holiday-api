package com.anggaarifandi.holidays.api;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		long started = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			if (!request.getRequestURI().startsWith("/actuator")) {
				long elapsedMs = (System.nanoTime() - started) / 1_000_000;
				log.info("{} {} -> {} {}ms", request.getMethod(), request.getRequestURI(),
						response.getStatus(), elapsedMs);
			}
		}
	}

}
