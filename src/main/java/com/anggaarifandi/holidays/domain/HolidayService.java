package com.anggaarifandi.holidays.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.anggaarifandi.holidays.api.InvalidRequestException;
import com.anggaarifandi.holidays.api.dto.CountryRefDto;
import com.anggaarifandi.holidays.api.dto.HolidayItemDto;
import com.anggaarifandi.holidays.api.dto.RecentHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.SharedHolidayDto;
import com.anggaarifandi.holidays.api.dto.SharedHolidaysResponse;
import com.anggaarifandi.holidays.api.dto.WeekdayCountItemDto;
import com.anggaarifandi.holidays.api.dto.WeekdayCountsResponse;
import com.anggaarifandi.holidays.client.NagerDateClient;
import com.anggaarifandi.holidays.client.NagerHolidayDto;

@Service
public class HolidayService {

	private final NagerDateClient nagerDateClient;
	private final CountryCodeNormalizer countryCodeNormalizer;
	private final ExecutorService holidayFetchExecutor;
	private final Clock clock;

	public HolidayService(
			NagerDateClient nagerDateClient,
			CountryCodeNormalizer countryCodeNormalizer,
			ExecutorService holidayFetchExecutor,
			Clock clock
	) {
		this.nagerDateClient = nagerDateClient;
		this.countryCodeNormalizer = countryCodeNormalizer;
		this.holidayFetchExecutor = holidayFetchExecutor;
		this.clock = clock;
	}

	public RecentHolidaysResponse getRecentHolidays(String countryCodeRaw, int limit) {
		if (limit < 1 || limit > 10) {
			throw new InvalidRequestException("limit must be between 1 and 10");
		}
		String countryCode = countryCodeNormalizer.normalize(countryCodeRaw);
		LocalDate today = LocalDate.now(clock);
		int currentYear = today.getYear();

		List<NagerHolidayDto> candidates = new ArrayList<>();
		candidates.addAll(publicHolidaysOnOrBefore(currentYear, countryCode, today));
		if (candidates.size() < limit) {
			candidates.addAll(publicHolidaysOnOrBefore(currentYear - 1, countryCode, today));
		}

		List<HolidayItemDto> holidays = candidates.stream()
				.sorted(Comparator.comparing(NagerHolidayDto::date).reversed())
				.limit(limit)
				.map(h -> new HolidayItemDto(h.date(), h.name(), h.localName()))
				.toList();

		return new RecentHolidaysResponse(countryCode, CountryEmoji.of(countryCode), holidays);
	}

	public WeekdayCountsResponse countWeekdayHolidays(Integer yearRaw, List<String> countryCodesRaw) {
		if (countryCodesRaw == null || countryCodesRaw.isEmpty()) {
			throw new InvalidRequestException("countries must contain at least one country code");
		}
		int year = yearRaw != null ? yearRaw : LocalDate.now(clock).getYear();
		validateYear(year);

		List<String> countries = countryCodesRaw.stream()
				.map(countryCodeNormalizer::normalize)
				.distinct()
				.toList();

		List<CompletableFuture<WeekdayCountItemDto>> futures = countries.stream()
				.map(code -> CompletableFuture.supplyAsync(
						() -> countForCountry(year, code),
						holidayFetchExecutor))
				.toList();

		List<WeekdayCountItemDto> results = futures.stream()
				.map(CompletableFuture::join)
				.sorted(Comparator.comparingInt(WeekdayCountItemDto::weekdayHolidayCount).reversed()
						.thenComparing(WeekdayCountItemDto::countryCode))
				.toList();

		return new WeekdayCountsResponse(year, results);
	}

	public SharedHolidaysResponse getSharedHolidays(Integer yearRaw, String countryARaw, String countryBRaw) {
		int year = yearRaw != null ? yearRaw : LocalDate.now(clock).getYear();
		validateYear(year);
		String countryA = countryCodeNormalizer.normalize(countryARaw);
		String countryB = countryCodeNormalizer.normalize(countryBRaw);
		if (countryA.equals(countryB)) {
			throw new InvalidRequestException("countryA and countryB must be different");
		}

		CompletableFuture<List<NagerHolidayDto>> futureA = CompletableFuture.supplyAsync(
				() -> publicHolidays(year, countryA), holidayFetchExecutor);
		CompletableFuture<List<NagerHolidayDto>> futureB = CompletableFuture.supplyAsync(
				() -> publicHolidays(year, countryB), holidayFetchExecutor);

		List<NagerHolidayDto> holidaysA = futureA.join();
		List<NagerHolidayDto> holidaysB = futureB.join();

		Map<LocalDate, NagerHolidayDto> byDateA = indexByDate(holidaysA);
		Set<LocalDate> datesA = byDateA.keySet();

		List<SharedHolidayDto> shared = new ArrayList<>();
		for (NagerHolidayDto holidayB : holidaysB) {
			LocalDate date = holidayB.date();
			if (!datesA.contains(date)) {
				continue;
			}
			NagerHolidayDto holidayA = byDateA.get(date);
			Map<String, String> localNames = new LinkedHashMap<>();
			localNames.put(countryA, holidayA.localName());
			localNames.put(countryB, holidayB.localName());
			Map<String, String> names = new LinkedHashMap<>();
			names.put(countryA, holidayA.name());
			names.put(countryB, holidayB.name());
			shared.add(new SharedHolidayDto(date, localNames, names));
		}

		shared.sort(Comparator.comparing(SharedHolidayDto::date));

		return new SharedHolidaysResponse(
				year,
				new CountryRefDto(countryA, CountryEmoji.of(countryA)),
				new CountryRefDto(countryB, CountryEmoji.of(countryB)),
				shared);
	}

	private WeekdayCountItemDto countForCountry(int year, String countryCode) {
		int count = (int) publicHolidays(year, countryCode).stream()
				.filter(HolidayFilters::isWeekdayPublic)
				.count();
		return new WeekdayCountItemDto(countryCode, CountryEmoji.of(countryCode), count);
	}

	private List<NagerHolidayDto> publicHolidays(int year, String countryCode) {
		return nagerDateClient.getPublicHolidays(year, countryCode).stream()
				.filter(HolidayFilters::isPublic)
				.toList();
	}

	private List<NagerHolidayDto> publicHolidaysOnOrBefore(int year, String countryCode, LocalDate today) {
		return publicHolidays(year, countryCode).stream()
				.filter(h -> !h.date().isAfter(today))
				.toList();
	}

	private static Map<LocalDate, NagerHolidayDto> indexByDate(List<NagerHolidayDto> holidays) {
		Map<LocalDate, NagerHolidayDto> map = new HashMap<>();
		for (NagerHolidayDto holiday : holidays) {
			map.putIfAbsent(holiday.date(), holiday);
		}
		return map;
	}

	private static void validateYear(int year) {
		if (year < 1920 || year > 2100) {
			throw new InvalidRequestException("year must be between 1920 and 2100");
		}
	}

	public List<String> parseCountryList(String countriesCsv) {
		if (countriesCsv == null || countriesCsv.isBlank()) {
			throw new InvalidRequestException("countries query parameter is required");
		}
		List<String> parts = List.of(countriesCsv.split(","));
		List<String> cleaned = parts.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(ArrayList::new));
		if (cleaned.isEmpty()) {
			throw new InvalidRequestException("countries must contain at least one country code");
		}
		return cleaned;
	}

}
