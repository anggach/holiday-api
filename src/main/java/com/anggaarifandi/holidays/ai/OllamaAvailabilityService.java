package com.anggaarifandi.holidays.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.anggaarifandi.holidays.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OllamaAvailabilityService {

	private static final Duration CACHE_TTL = Duration.ofSeconds(10);

	private final AppProperties appProperties;
	private final ObjectMapper objectMapper;
	private final String baseUrl;
	private final HttpClient httpClient;
	private final AtomicReference<CachedStatus> cache = new AtomicReference<>();

	public OllamaAvailabilityService(AppProperties appProperties, ObjectMapper objectMapper) {
		this.appProperties = appProperties;
		this.objectMapper = objectMapper;
		String configured = appProperties.ai().baseUrl();
		this.baseUrl = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(appProperties.ai().healthTimeout())
				.build();
	}

	public Status check() {
		CachedStatus cached = cache.get();
		if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
			return cached.status();
		}
		Status status = probe();
		cache.set(new CachedStatus(status, Instant.now().plus(CACHE_TTL)));
		return status;
	}

	public boolean isReady() {
		Status status = check();
		return status.reachable() && status.modelPresent();
	}

	public void assertReady() {
		Status status = check();
		if (!status.reachable() || !status.modelPresent()) {
			throw new AiUnavailableException(
					"Ollama is not reachable or required model is missing.",
					setupSteps());
		}
	}

	public List<String> setupSteps() {
		String model = appProperties.ai().expectedModel();
		return List.of(
				"Install Ollama from https://ollama.com",
				"Run: ollama serve",
				"Run: ollama pull " + model,
				"Verify: GET /actuator/health/ollamaAi");
	}

	private Status probe() {
		String model = appProperties.ai().expectedModel();
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/api/tags"))
					.timeout(appProperties.ai().healthTimeout())
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return new Status(false, false, baseUrl, model, "Ollama returned HTTP " + response.statusCode());
			}
			JsonNode root = objectMapper.readTree(response.body());
			boolean modelPresent = false;
			JsonNode models = root.path("models");
			if (models.isArray()) {
				for (JsonNode node : models) {
					String name = node.path("name").asText("");
					if (name.equals(model) || name.startsWith(model + ":")) {
						modelPresent = true;
						break;
					}
				}
			}
			String hint = modelPresent
					? "Ollama ready"
					: "Model not found. Run: ollama pull " + model;
			return new Status(true, modelPresent, baseUrl, model, hint);
		}
		catch (Exception ex) {
			return new Status(false, false, baseUrl, model,
					"Start Ollama and pull " + model + " (" + ex.getClass().getSimpleName() + ")");
		}
	}

	public record Status(
			boolean reachable,
			boolean modelPresent,
			String baseUrl,
			String expectedModel,
			String hint
	) {
		public Map<String, Object> asDetails() {
			return Map.of(
					"reachable", reachable,
					"modelPresent", modelPresent,
					"baseUrl", baseUrl,
					"expectedModel", expectedModel,
					"hint", hint);
		}
	}

	private record CachedStatus(Status status, Instant expiresAt) {
	}

}
