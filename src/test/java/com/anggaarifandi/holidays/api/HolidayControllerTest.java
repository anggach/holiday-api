package com.anggaarifandi.holidays.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.anggaarifandi.holidays.api.dto.CountryRefDto;
import com.anggaarifandi.holidays.api.dto.HolidayItemDto;
import com.anggaarifandi.holidays.api.dto.RecentHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.SharedHolidayDto;
import com.anggaarifandi.holidays.api.dto.SharedHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.WeekdayCountItemDto;
import com.anggaarifandi.holidays.api.dto.WeekdayCountsResponse;
import com.anggaarifandi.holidays.config.AppProperties;
import com.anggaarifandi.holidays.domain.HolidayService;

@WebMvcTest(controllers = HolidayController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
		HolidayControllerTest.TestConfig.class,
		ApiExceptionHandler.class,
		ApiKeyAuthFilter.class,
		RateLimitFilter.class,
		RequestIdFilter.class
})
class HolidayControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	HolidayService holidayService;

	@TestConfiguration
	static class TestConfig {
		@Bean
		AppProperties appProperties() {
			return new AppProperties(
					"test-api-key",
					new AppProperties.RateLimit(1000, 1000, Duration.ofMinutes(1)),
					new AppProperties.Nager("http://localhost:9", Duration.ofSeconds(1), Duration.ofSeconds(1), 1),
					new AppProperties.Ai(false, "llama3.1", Duration.ofSeconds(1), false, "http://localhost:9"),
					new AppProperties.Error(true, "https://api.holidays.local/problems"),
					new AppProperties.Cache(500, Duration.ofHours(24)));
		}
	}

	@Test
	void recentRequiresApiKey() throws Exception {
		mockMvc.perform(get("/api/v1/holidays/NL/recent"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void recentHappyPath() throws Exception {
		when(holidayService.getRecentHolidays(eq("NL"), eq(3)))
				.thenReturn(new RecentHolidaysResponse("NL", "🇳🇱", List.of(
						new HolidayItemDto(LocalDate.of(2026, 4, 6), "Easter Monday", "Tweede Paasdag"))));

		mockMvc.perform(get("/api/v1/holidays/NL/recent")
						.header("X-API-Key", "test-api-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.countryCode").value("NL"))
				.andExpect(jsonPath("$.holidays[0].name").value("Easter Monday"));
	}

	@Test
	void sharedHappyPath() throws Exception {
		when(holidayService.getSharedHolidays(eq(2026), eq("NL"), eq("DE")))
				.thenReturn(new SharedHolidaysResponse(
						2026,
						new CountryRefDto("NL", "🇳🇱"),
						new CountryRefDto("DE", "🇩🇪"),
						List.of(new SharedHolidayDto(
								LocalDate.of(2026, 1, 1),
								Map.of("NL", "Nieuwjaarsdag", "DE", "Neujahr"),
								Map.of("NL", "New Year's Day", "DE", "New Year's Day")))));

		mockMvc.perform(get("/api/v1/holidays/shared")
						.param("year", "2026")
						.param("countryA", "NL")
						.param("countryB", "DE")
						.header("X-API-Key", "test-api-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shared[0].date").value("2026-01-01"));
	}

	@Test
	void weekdayCountsHappyPath() throws Exception {
		when(holidayService.parseCountryList("NL,DE")).thenReturn(List.of("NL", "DE"));
		when(holidayService.countWeekdayHolidays(any(), anyList()))
				.thenReturn(new WeekdayCountsResponse(2026, List.of(
						new WeekdayCountItemDto("DE", "🇩🇪", 10),
						new WeekdayCountItemDto("NL", "🇳🇱", 8))));

		mockMvc.perform(get("/api/v1/holidays/weekday-counts")
						.param("year", "2026")
						.param("countries", "NL,DE")
						.header("X-API-Key", "test-api-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results[0].countryCode").value("DE"));
	}

}
