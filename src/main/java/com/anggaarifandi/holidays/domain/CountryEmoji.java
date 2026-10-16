package com.anggaarifandi.holidays.domain;

public final class CountryEmoji {

	private CountryEmoji() {
	}

	/**
	 * Converts an ISO 3166-1 alpha-2 code to a regional-indicator flag emoji.
	 */
	public static String of(String countryCode) {
		if (countryCode == null || countryCode.length() != 2) {
			return "";
		}
		String upper = countryCode.toUpperCase();
		int first = Character.codePointAt(upper, 0) - 'A' + 0x1F1E6;
		int second = Character.codePointAt(upper, 1) - 'A' + 0x1F1E6;
		return new String(Character.toChars(first)) + new String(Character.toChars(second));
	}

}
