package com.anggaarifandi.holidays.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class NagerDateClientTest {

	MockWebServer server;
	NagerDateClient client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		RestClient restClient = RestClient.builder().baseUrl(server.url("/").toString()).build();
		client = new NagerDateClient(restClient);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void fetchesHolidays() throws Exception {
		server.enqueue(new MockResponse()
				.setBody("""
						[{"date":"2026-01-01","localName":"Nieuwjaarsdag","name":"New Year's Day",
						"countryCode":"NL","fixed":false,"global":true,"counties":null,"launchYear":null,
						"types":["Public"]}]
						""")
				.addHeader("Content-Type", "application/json"));

		List<NagerHolidayDto> holidays = client.getPublicHolidays(2026, "NL");

		assertThat(holidays).hasSize(1);
		assertThat(holidays.get(0).localName()).isEqualTo("Nieuwjaarsdag");
		assertThat(server.takeRequest(1, TimeUnit.SECONDS).getPath())
				.isEqualTo("/PublicHolidays/2026/NL");
	}

	@Test
	void maps404ToCountryNotFound() {
		server.enqueue(new MockResponse().setResponseCode(404));

		assertThatThrownBy(() -> client.getPublicHolidays(2026, "XX"))
				.isInstanceOf(CountryNotFoundException.class);
	}

	@Test
	void maps5xxToUpstream() {
		server.enqueue(new MockResponse().setResponseCode(503));

		assertThatThrownBy(() -> client.getPublicHolidays(2026, "NL"))
				.isInstanceOf(UpstreamException.class);
	}

}
