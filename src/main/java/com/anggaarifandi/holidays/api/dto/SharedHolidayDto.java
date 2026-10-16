package com.anggaarifandi.holidays.api.dto;

import java.time.LocalDate;
import java.util.Map;

public record SharedHolidayDto(
		LocalDate date,
		Map<String, String> localNames,
		Map<String, String> names
) {
}
