package com.anggaarifandi.holidays.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.anggaarifandi.holidays.client.NagerHolidayDto;

class HolidayFiltersTest {

	@Test
	void detectsPublicType() {
		assertThat(HolidayFilters.isPublic(holiday(LocalDate.of(2026, 1, 1), List.of("Public")))).isTrue();
		assertThat(HolidayFilters.isPublic(holiday(LocalDate.of(2026, 1, 1), List.of("Observance")))).isFalse();
	}

	@Test
	void weekdayExcludesWeekend() {
		assertThat(HolidayFilters.isWeekday(LocalDate.of(2026, 1, 1))).isTrue(); // Thursday
		assertThat(HolidayFilters.isWeekday(LocalDate.of(2026, 1, 3))).isFalse(); // Saturday
		assertThat(HolidayFilters.isWeekday(LocalDate.of(2026, 1, 4))).isFalse(); // Sunday
	}

	private static NagerHolidayDto holiday(LocalDate date, List<String> types) {
		return new NagerHolidayDto(date, "Local", "Name", "NL", true, true, null, null, types);
	}

}
