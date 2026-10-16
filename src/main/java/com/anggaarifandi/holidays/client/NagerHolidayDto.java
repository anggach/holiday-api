package com.anggaarifandi.holidays.client;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NagerHolidayDto(
		LocalDate date,
		String localName,
		String name,
		String countryCode,
		boolean fixed,
		boolean global,
		List<String> counties,
		Integer launchYear,
		List<String> types
) {
}
