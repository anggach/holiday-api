package com.anggaarifandi.holidays.client;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NagerDateClient {

	private static final ParameterizedTypeReference<List<NagerHolidayDto>> HOLIDAY_LIST =
			new ParameterizedTypeReference<>() {
			};

	private static final ParameterizedTypeReference<List<NagerCountryDto>> COUNTRY_LIST =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;

	public NagerDateClient(RestClient nagerRestClient) {
		this.restClient = nagerRestClient;
	}

	@Cacheable(cacheNames = "holidays", key = "#year + ':' + #countryCode")
	@Retryable(
			retryFor = { UpstreamException.class },
			maxAttemptsExpression = "${app.nager.max-attempts}",
			backoff = @Backoff(delay = 200, multiplier = 2.0)
	)
	public List<NagerHolidayDto> getPublicHolidays(int year, String countryCode) {
		try {
			List<NagerHolidayDto> holidays = restClient.get()
					.uri("/PublicHolidays/{year}/{countryCode}", year, countryCode)
					.retrieve()
					.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
						if (response.getStatusCode().value() == 404) {
							throw new CountryNotFoundException(countryCode);
						}
						throw new UpstreamException(
								"Nager.Date client error " + response.getStatusCode().value()
										+ " for " + countryCode + "/" + year);
					})
					.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
						throw new UpstreamException(
								"Nager.Date server error " + response.getStatusCode().value()
										+ " for " + countryCode + "/" + year);
					})
					.body(HOLIDAY_LIST);
			return holidays == null ? List.of() : List.copyOf(holidays);
		}
		catch (CountryNotFoundException ex) {
			throw ex;
		}
		catch (RestClientResponseException ex) {
			throw new UpstreamException("Nager.Date HTTP " + ex.getStatusCode().value(), ex);
		}
		catch (RestClientException ex) {
			throw new UpstreamException("Failed to reach Nager.Date API", ex);
		}
	}

	@Cacheable(cacheNames = "countries", key = "'all'")
	@Retryable(
			retryFor = { UpstreamException.class },
			maxAttemptsExpression = "${app.nager.max-attempts}",
			backoff = @Backoff(delay = 200, multiplier = 2.0)
	)
	public List<NagerCountryDto> getAvailableCountries() {
		try {
			List<NagerCountryDto> countries = restClient.get()
					.uri("/AvailableCountries")
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new UpstreamException(
								"Nager.Date error fetching available countries: "
										+ response.getStatusCode().value());
					})
					.body(COUNTRY_LIST);
			return countries == null ? List.of() : List.copyOf(countries);
		}
		catch (RestClientException ex) {
			throw new UpstreamException("Failed to reach Nager.Date API for countries", ex);
		}
	}

	public boolean isKnownCountry(String countryCode) {
		return getAvailableCountries().stream()
				.anyMatch(c -> c.countryCode().equalsIgnoreCase(countryCode));
	}

}
