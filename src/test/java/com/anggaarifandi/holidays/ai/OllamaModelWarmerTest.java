package com.anggaarifandi.holidays.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anggaarifandi.holidays.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class OllamaModelWarmerTest {

	MockWebServer server;
	ObjectMapper objectMapper;
	OllamaAvailabilityService availability;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		objectMapper = new ObjectMapper();
		AppProperties props = new AppProperties(
				"key",
				new AppProperties.RateLimit(60, 60, Duration.ofMinutes(1)),
				new AppProperties.Nager("http://localhost", Duration.ofSeconds(1), Duration.ofSeconds(1), 1),
				new AppProperties.Ai(false, "llama3.1", Duration.ofSeconds(2), false, server.url("/").toString()),
				new AppProperties.Error(false, "https://api.holidays.local/problems"),
				new AppProperties.Cache(500, Duration.ofHours(24)));
		availability = new OllamaAvailabilityService(props, objectMapper);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void warmLoadsModelWithChatContext() throws Exception {
		server.enqueue(new MockResponse()
				.setHeader("Content-Type", "application/json")
				.setBody("{\"models\":[{\"name\":\"llama3.1:latest\"}]}"));
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

		warmer(true).warm();

		assertThat(server.takeRequest().getPath()).isEqualTo("/api/tags");
		RecordedRequest generate = server.takeRequest();
		assertThat(generate.getPath()).isEqualTo("/api/generate");
		JsonNode body = objectMapper.readTree(generate.getBody().readUtf8());
		assertThat(body.path("model").asText()).isEqualTo("llama3.1");
		assertThat(body.path("keep_alive").asText()).isEqualTo("30m");
		assertThat(body.path("options").path("num_ctx").asInt()).isEqualTo(4096);
		assertThat(body.path("options").path("num_predict").asInt()).isEqualTo(1);
	}

	@Test
	void warmSkipsWhenModelIsMissing() throws Exception {
		server.enqueue(new MockResponse()
				.setHeader("Content-Type", "application/json")
				.setBody("{\"models\":[]}"));

		warmer(true).warm();

		assertThat(server.takeRequest().getPath()).isEqualTo("/api/tags");
		assertThat(server.getRequestCount()).isEqualTo(1);
	}

	@Test
	void warmSkipsWhenDisabled() {
		warmer(false).warm();
		assertThat(server.getRequestCount()).isZero();
	}

	private OllamaModelWarmer warmer(boolean enabled) {
		return new OllamaModelWarmer(
				enabled,
				server.url("/").toString(),
				"llama3.1",
				4096,
				"30m",
				availability,
				objectMapper);
	}

}
