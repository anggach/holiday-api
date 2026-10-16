package com.anggaarifandi.holidays.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import com.anggaarifandi.holidays.client.NagerHolidayDto;

public final class HolidayFilters {

	private HolidayFilters() {
	}

	public static boolean isPublic(NagerHolidayDto holiday) {
		List<String> types = holiday.types();
		return types != null && types.stream().anyMatch("Public"::equalsIgnoreCase);
	}

	public static boolean isWeekday(LocalDate date) {
		DayOfWeek day = date.getDayOfWeek();
		return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
	}

	public static boolean isWeekdayPublic(NagerHolidayDto holiday) {
		return isPublic(holiday) && isWeekday(holiday.date());
	}

}
