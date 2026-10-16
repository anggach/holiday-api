package com.anggaarifandi.holidays.api.dto;

import java.util.List;

public record WeekdayCountsResponse(int year, List<WeekdayCountItemDto> results) {
}
