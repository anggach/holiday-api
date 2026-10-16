package com.anggaarifandi.holidays.api.dto;

import java.util.List;

public record RecentHolidaysResponse(
		String countryCode,
		String countryEmoji,
		List<HolidayItemDto> holidays
) {
}
