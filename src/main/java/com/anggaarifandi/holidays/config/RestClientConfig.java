package com.anggaarifandi.holidays.config;

import java.time.Duration;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class RestClientConfig {

	private static final Duration OLLAMA_CONNECT_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration OLLAMA_READ_TIMEOUT = Duration.ofMinutes(3);

	@Bean
	RestClient.Builder restClientBuilder(ObservationRegistry observationRegistry) {
		// Spring AI Ollama reuses this builder. Keep the read timeout long enough
		// for a local tool-calling model; Nager uses its own client below.
		return observedBuilder(observationRegistry, OLLAMA_CONNECT_TIMEOUT, OLLAMA_READ_TIMEOUT);
	}

	@Bean
	RestClient nagerRestClient(AppProperties properties, ObservationRegistry observationRegistry) {
		return observedBuilder(
				observationRegistry,
				properties.nager().connectTimeout(),
				properties.nager().readTimeout())
				.baseUrl(properties.nager().baseUrl())
				.build();
	}

	private static RestClient.Builder observedBuilder(
			ObservationRegistry observationRegistry,
			Duration connectTimeout,
			Duration readTimeout
	) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
				.withConnectTimeout(connectTimeout)
				.withReadTimeout(readTimeout);
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
		return RestClient.builder()
				.requestFactory(requestFactory)
				.observationRegistry(observationRegistry);
	}

	@Bean(destroyMethod = "shutdown")
	java.util.concurrent.ExecutorService holidayFetchExecutor() {
		return java.util.concurrent.Executors.newFixedThreadPool(
				Math.max(4, Runtime.getRuntime().availableProcessors()),
				r -> {
					Thread t = new Thread(r, "holiday-fetch");
					t.setDaemon(true);
					return t;
				});
	}

}
