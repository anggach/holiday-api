package com.anggaarifandi.holidays.api.dto;

public record WeekdayCountItemDto(
		String countryCode,
		String countryEmoji,
		int weekdayHolidayCount
) {
}
