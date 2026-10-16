package com.anggaarifandi.holidays.ai;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("ollamaAi")
public class OllamaHealthIndicator implements HealthIndicator {

	private final OllamaAvailabilityService availabilityService;

	public OllamaHealthIndicator(OllamaAvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	@Override
	public Health health() {
		// AI is optional: keep contributor UP so overall app health stays UP.
		// Clients should read details.ready (or call chat and handle 503).
		OllamaAvailabilityService.Status status = availabilityService.check();
		boolean ready = status.reachable() && status.modelPresent();
		return Health.up()
				.withDetails(status.asDetails())
				.withDetail("ready", ready)
				.build();
	}

}
