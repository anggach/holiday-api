package com.anggaarifandi.holidays.ai;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.anggaarifandi.holidays.api.dto.RecentHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.SharedHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.WeekdayCountsResponse;
import com.anggaarifandi.holidays.domain.HolidayService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class HolidayTools {

	private final HolidayService holidayService;
	private final ObjectMapper objectMapper;
	private final ToolCallLog toolCallLog;

	public HolidayTools(HolidayService holidayService, ObjectMapper objectMapper, ToolCallLog toolCallLog) {
		this.holidayService = holidayService;
		this.objectMapper = objectMapper;
		this.toolCallLog = toolCallLog;
	}

	@Tool(description = """
			Return the most recently celebrated public holidays for one country.
			Use only for questions about last/recent/past holidays in a single country.
			countryCode must be ISO 3166-1 alpha-2 (e.g. NL, DE, ID).
			""")
	public String getRecentHolidays(
			@ToolParam(description = "ISO 3166-1 alpha-2 country code") String countryCode,
			@ToolParam(description = "Number of holidays to return, default 3, max 10", required = false)
			Integer limit
	) {
		int resolvedLimit = limit == null ? 3 : limit;
		toolCallLog.record("getRecentHolidays");
		RecentHolidaysResponse response = holidayService.getRecentHolidays(countryCode, resolvedLimit);
		return toJson(response);
	}

	@Tool(description = """
			Count public holidays that fall on weekdays (Mon-Fri) for one or more countries in a year.
			Use for comparisons of weekday holiday counts. Sort is descending by count.
			countryCodesCsv is a comma-separated list of ISO alpha-2 codes, e.g. NL,DE,FR.
			""")
	public String countWeekdayHolidays(
			@ToolParam(description = "Year, e.g. 2026") int year,
			@ToolParam(description = "Comma-separated ISO alpha-2 country codes") String countryCodesCsv
	) {
		List<String> codes = Arrays.stream(countryCodesCsv.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		toolCallLog.record("countWeekdayHolidays");
		WeekdayCountsResponse response = holidayService.countWeekdayHolidays(year, codes);
		return toJson(response);
	}

	@Tool(description = """
			Return dates that are public holidays in BOTH countries for a given year, with local names.
			Use only when exactly two countries are compared for shared holiday dates.
			Both country codes must be ISO 3166-1 alpha-2.
			""")
	public String getSharedHolidays(
			@ToolParam(description = "Year, e.g. 2026") int year,
			@ToolParam(description = "First ISO alpha-2 country code") String countryA,
			@ToolParam(description = "Second ISO alpha-2 country code") String countryB
	) {
		toolCallLog.record("getSharedHolidays");
		SharedHolidaysResponse response = holidayService.getSharedHolidays(year, countryA, countryB);
		return toJson(response);
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			return String.valueOf(value);
		}
	}

}
