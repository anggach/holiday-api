package com.anggaarifandi.holidays.api.dto;

import java.util.List;

public record SharedHolidaysResponse(
		int year,
		CountryRefDto countryA,
		CountryRefDto countryB,
		List<SharedHolidayDto> shared
) {
}
