package com.anggaarifandi.holidays.domain;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.anggaarifandi.holidays.api.InvalidRequestException;
import com.anggaarifandi.holidays.client.CountryNotFoundException;
import com.anggaarifandi.holidays.client.NagerDateClient;

@Component
public class CountryCodeNormalizer {

	private final NagerDateClient nagerDateClient;

	public CountryCodeNormalizer(NagerDateClient nagerDateClient) {
		this.nagerDateClient = nagerDateClient;
	}

	public String normalize(String raw) {
		if (!StringUtils.hasText(raw)) {
			throw new InvalidRequestException("Country code must not be blank");
		}
		String code = raw.trim().toUpperCase();
		if (!code.matches("[A-Z]{2}")) {
			throw new InvalidRequestException(
					"Country code must be ISO 3166-1 alpha-2 (e.g. NL), got: " + raw);
		}
		if (!nagerDateClient.isKnownCountry(code)) {
			throw new CountryNotFoundException(code);
		}
		return code;
	}

}
