package com.anggaarifandi.holidays.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		String apiKey,
		RateLimit rateLimit,
		Nager nager,
		Ai ai,
		Error error,
		Cache cache
) {

	public record RateLimit(long capacity, long refillTokens, Duration refillDuration) {
	}

	public record Nager(String baseUrl, Duration connectTimeout, Duration readTimeout, int maxAttempts) {
	}

	public record Ai(boolean required, String expectedModel, Duration healthTimeout, boolean warmup, String baseUrl) {
	}

	public record Error(boolean includeDetail, String problemTypeBase) {

		public String problemType(String suffix) {
			String base = problemTypeBase.endsWith("/")
					? problemTypeBase.substring(0, problemTypeBase.length() - 1)
					: problemTypeBase;
			return base + "/" + suffix;
		}

	}

	public record Cache(long maximumSize, Duration expireAfterWrite) {
	}

}
