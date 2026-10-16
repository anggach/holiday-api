package com.anggaarifandi.holidays.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CountryEmojiTest {

	@Test
	void buildsFlagEmoji() {
		assertThat(CountryEmoji.of("NL")).isEqualTo("🇳🇱");
		assertThat(CountryEmoji.of("id")).isEqualTo("🇮🇩");
		assertThat(CountryEmoji.of("X")).isEmpty();
	}

}
