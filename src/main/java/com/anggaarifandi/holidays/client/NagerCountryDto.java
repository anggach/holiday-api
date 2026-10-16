package com.anggaarifandi.holidays.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NagerCountryDto(String countryCode, String name) {
}
