package com.anggaarifandi.holidays.api;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anggaarifandi.holidays.api.dto.RecentHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.SharedHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.WeekdayCountsResponse;
import com.anggaarifandi.holidays.domain.HolidayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping(path = "/api/v1/holidays", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Holidays")
@SecurityRequirement(name = "ApiKeyAuth")
public class HolidayController {

	private final HolidayService holidayService;

	public HolidayController(HolidayService holidayService) {
		this.holidayService = holidayService;
	}

	@GetMapping("/{countryCode}/recent")
	@Operation(summary = "Last celebrated public holidays for a country")
	public RecentHolidaysResponse recent(
			@PathVariable String countryCode,
			@RequestParam(defaultValue = "3") @Min(1) @Max(10) int limit
	) {
		return holidayService.getRecentHolidays(countryCode, limit);
	}

	@GetMapping("/weekday-counts")
	@Operation(summary = "Weekday public holiday counts by country (descending)")
	public WeekdayCountsResponse weekdayCounts(
			@RequestParam(required = false) Integer year,
			@RequestParam String countries
	) {
		return holidayService.countWeekdayHolidays(year, holidayService.parseCountryList(countries));
	}

	@GetMapping("/shared")
	@Operation(summary = "Shared public holiday dates between two countries")
	public SharedHolidaysResponse shared(
			@RequestParam(required = false) Integer year,
			@RequestParam String countryA,
			@RequestParam String countryB
	) {
		return holidayService.getSharedHolidays(year, countryA, countryB);
	}

}
