package com.anggaarifandi.holidays.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OllamaModelWarmer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(OllamaModelWarmer.class);

	private static final Duration WARMUP_TIMEOUT = Duration.ofSeconds(60);

	private static final int WARMUP_PREDICT_TOKENS = 1;

	private final boolean enabled;
	private final String baseUrl;
	private final String model;
	private final int numCtx;
	private final String keepAlive;
	private final OllamaAvailabilityService availabilityService;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public OllamaModelWarmer(
			@Value("${app.ai.warmup:true}") boolean enabled,
			@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
			@Value("${spring.ai.ollama.chat.options.model:llama3.1}") String model,
			@Value("${spring.ai.ollama.chat.options.num-ctx:4096}") int numCtx,
			@Value("${spring.ai.ollama.chat.options.keep-alive:30m}") String keepAlive,
			OllamaAvailabilityService availabilityService,
			ObjectMapper objectMapper
	) {
		this.enabled = enabled;
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.model = model;
		this.numCtx = numCtx;
		this.keepAlive = keepAlive;
		this.availabilityService = availabilityService;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!enabled) {
			return;
		}
		Thread thread = new Thread(this::warm, "ollama-warmup");
		thread.setDaemon(true);
		thread.start();
	}

	void warm() {
		if (!enabled) {
			return;
		}
		if (!availabilityService.isReady()) {
			log.info("Skipping Ollama warmup; model is not ready");
			return;
		}
		long started = System.nanoTime();
		try {
			String body = objectMapper.writeValueAsString(Map.of(
					"model", model,
					"prompt", "ok",
					"stream", false,
					"keep_alive", keepAlive,
					"options", Map.of(
							"num_ctx", numCtx,
							"num_predict", WARMUP_PREDICT_TOKENS,
							"temperature", 0)));
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/api/generate"))
					.timeout(WARMUP_TIMEOUT)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			long elapsedMs = (System.nanoTime() - started) / 1_000_000;
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				log.info("Ollama warmup finished in {}ms (num_ctx={}, keep_alive={})", elapsedMs, numCtx, keepAlive);
			}
			else {
				log.warn("Ollama warmup returned HTTP {} after {}ms", response.statusCode(), elapsedMs);
			}
		}
		catch (Exception ex) {
			log.warn("Ollama warmup failed: {}", ex.toString());
		}
	}

}
