package com.anggaarifandi.holidays.client;

public class CountryNotFoundException extends RuntimeException {

	private final String countryCode;

	public CountryNotFoundException(String countryCode) {
		super("Unknown or unsupported country code: " + countryCode);
		this.countryCode = countryCode;
	}

	public String getCountryCode() {
		return countryCode;
	}

}
