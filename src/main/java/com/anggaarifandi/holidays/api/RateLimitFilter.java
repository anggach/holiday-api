package com.anggaarifandi.holidays.api;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.anggaarifandi.holidays.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(30)
public class RateLimitFilter extends OncePerRequestFilter {

	private final AppProperties appProperties;
	private final ObjectMapper objectMapper;
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	public RateLimitFilter(AppProperties appProperties, ObjectMapper objectMapper) {
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
		String key = request.getHeader(ApiKeyAuthFilter.HEADER);
		if (key == null || key.isBlank()) {
			key = request.getRemoteAddr();
		}
		Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
		if (!probe.isConsumed()) {
			long retryAfter = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
			response.setStatus(429);
			response.setHeader("Retry-After", String.valueOf(retryAfter));
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			var body = java.util.Map.of(
					"type", appProperties.error().problemType("rate-limit"),
					"title", "Rate Limit Exceeded",
					"status", 429,
					"detail", "Rate limit exceeded. Try again in " + retryAfter + " seconds.",
					"instance", request.getRequestURI(),
					"retryAfterSeconds", retryAfter);
			objectMapper.writeValue(response.getOutputStream(), body);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private Bucket newBucket() {
		var rl = appProperties.rateLimit();
		Bandwidth limit = Bandwidth.builder()
				.capacity(rl.capacity())
				.refillGreedy(rl.refillTokens(), rl.refillDuration())
				.build();
		return Bucket.builder().addLimit(limit).build();
	}

}
