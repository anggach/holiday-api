package com.anggaarifandi.holidays.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anggaarifandi.holidays.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class OllamaAvailabilityServiceTest {

	MockWebServer server;
	OllamaAvailabilityService service;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		String baseUrl = server.url("/").toString().replaceAll("/$", "");
		AppProperties props = new AppProperties(
				"key",
				new AppProperties.RateLimit(60, 60, Duration.ofMinutes(1)),
				new AppProperties.Nager("http://localhost", Duration.ofSeconds(1), Duration.ofSeconds(1), 1),
				new AppProperties.Ai(false, "llama3.1", Duration.ofSeconds(2), false, baseUrl),
				new AppProperties.Error(false, "https://api.holidays.local/problems"),
				new AppProperties.Cache(500, Duration.ofHours(24)));
		service = new OllamaAvailabilityService(props, new ObjectMapper());
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void readyWhenModelPresent() {
		server.enqueue(new MockResponse()
				.setBody("{\"models\":[{\"name\":\"llama3.1:latest\"}]}")
				.addHeader("Content-Type", "application/json"));

		assertThat(service.isReady()).isTrue();
	}

	@Test
	void notReadyWhenModelMissing() {
		server.enqueue(new MockResponse()
				.setBody("{\"models\":[{\"name\":\"mistral:latest\"}]}")
				.addHeader("Content-Type", "application/json"));

		assertThat(service.isReady()).isFalse();
		assertThatThrownBy(service::assertReady).isInstanceOf(AiUnavailableException.class);
	}

}
