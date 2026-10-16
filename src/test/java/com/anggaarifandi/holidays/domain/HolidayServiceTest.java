package com.anggaarifandi.holidays.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anggaarifandi.holidays.api.InvalidRequestException;
import com.anggaarifandi.holidays.api.dto.RecentHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.SharedHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.WeekdayCountsResponse;
import com.anggaarifandi.holidays.client.NagerDateClient;
import com.anggaarifandi.holidays.client.NagerHolidayDto;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

	@Mock
	NagerDateClient nagerDateClient;

	CountryCodeNormalizer normalizer;
	HolidayService service;
	java.util.concurrent.ExecutorService executor;

	@BeforeEach
	void setUp() {
		when(nagerDateClient.isKnownCountry(anyString())).thenAnswer(inv -> {
			String code = inv.getArgument(0);
			return List.of("NL", "DE", "FR").contains(code);
		});
		normalizer = new CountryCodeNormalizer(nagerDateClient);
		executor = Executors.newFixedThreadPool(2);
		Clock clock = Clock.fixed(Instant.parse("2026-04-10T12:00:00Z"), ZoneOffset.UTC);
		service = new HolidayService(nagerDateClient, normalizer, executor, clock);
	}

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void recentHolidaysTakesLastThreeOnOrBeforeToday() {
		when(nagerDateClient.getPublicHolidays(eq(2026), eq("NL"))).thenReturn(List.of(
				publicHoliday("2026-01-01", "New Year", "Nieuwjaar"),
				publicHoliday("2026-04-05", "Easter", "Pasen"),
				publicHoliday("2026-04-06", "Easter Monday", "2e Pasen"),
				publicHoliday("2026-04-27", "Kings Day", "Koningsdag"),
				publicHoliday("2026-12-25", "Christmas", "Kerst")));

		RecentHolidaysResponse response = service.getRecentHolidays("nl", 3);

		assertThat(response.countryCode()).isEqualTo("NL");
		assertThat(response.holidays()).extracting(h -> h.date().toString())
				.containsExactly("2026-04-06", "2026-04-05", "2026-01-01");
	}

	@Test
	void recentHolidaysFallsBackToPreviousYear() {
		Clock early = Clock.fixed(Instant.parse("2026-01-02T12:00:00Z"), ZoneOffset.UTC);
		service = new HolidayService(nagerDateClient, normalizer, executor, early);

		when(nagerDateClient.getPublicHolidays(eq(2026), eq("NL"))).thenReturn(List.of(
				publicHoliday("2026-01-01", "New Year", "Nieuwjaar")));
		when(nagerDateClient.getPublicHolidays(eq(2025), eq("NL"))).thenReturn(List.of(
				publicHoliday("2025-12-25", "Christmas", "Kerst"),
				publicHoliday("2025-12-26", "Boxing", "2e Kerst")));

		RecentHolidaysResponse response = service.getRecentHolidays("NL", 3);

		assertThat(response.holidays()).extracting(h -> h.date().toString())
				.containsExactly("2026-01-01", "2025-12-26", "2025-12-25");
	}

	@Test
	void weekdayCountsSortedDescending() {
		when(nagerDateClient.getPublicHolidays(eq(2026), eq("NL"))).thenReturn(List.of(
				publicHoliday("2026-01-01", "NY", "NY"), // Thursday
				publicHoliday("2026-01-03", "Sat", "Sat"))); // Saturday ignored
		when(nagerDateClient.getPublicHolidays(eq(2026), eq("DE"))).thenReturn(List.of(
				publicHoliday("2026-01-01", "NY", "NY"),
				publicHoliday("2026-01-02", "Fri", "Fri"),
				publicHoliday("2026-05-01", "Labour", "Tag"))); // Friday

		WeekdayCountsResponse response = service.countWeekdayHolidays(2026, List.of("NL", "DE"));

		assertThat(response.results()).extracting(r -> r.countryCode() + ":" + r.weekdayHolidayCount())
				.containsExactly("DE:3", "NL:1");
	}

	@Test
	void sharedUsesSetIntersectionByDate() {
		when(nagerDateClient.getPublicHolidays(eq(2026), eq("NL"))).thenReturn(List.of(
				publicHoliday("2026-01-01", "New Year's Day", "Nieuwjaarsdag"),
				publicHoliday("2026-04-27", "King's Day", "Koningsdag")));
		when(nagerDateClient.getPublicHolidays(eq(2026), eq("DE"))).thenReturn(List.of(
				publicHoliday("2026-01-01", "New Year's Day", "Neujahr"),
				publicHoliday("2026-05-01", "Labour Day", "Tag der Arbeit")));

		SharedHolidaysResponse response = service.getSharedHolidays(2026, "NL", "DE");

		assertThat(response.shared()).hasSize(1);
		assertThat(response.shared().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
		assertThat(response.shared().get(0).localNames())
				.containsEntry("NL", "Nieuwjaarsdag")
				.containsEntry("DE", "Neujahr");
	}

	@Test
	void rejectsSameCountriesForShared() {
		assertThatThrownBy(() -> service.getSharedHolidays(2026, "NL", "nl"))
				.isInstanceOf(InvalidRequestException.class);
	}

	private static NagerHolidayDto publicHoliday(String date, String name, String localName) {
		return new NagerHolidayDto(
				LocalDate.parse(date), localName, name, "XX", true, true, null, null, List.of("Public"));
	}

}
