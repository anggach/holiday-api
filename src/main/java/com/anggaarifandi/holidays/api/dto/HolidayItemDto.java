package com.anggaarifandi.holidays.api.dto;

import java.time.LocalDate;

public record HolidayItemDto(LocalDate date, String name, String localName) {
}
