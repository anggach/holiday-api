package com.anggaarifandi.holidays.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CacheConfig {

	@Bean
	CacheManager cacheManager(AppProperties appProperties) {
		AppProperties.Cache cache = appProperties.cache();
		CaffeineCacheManager manager = new CaffeineCacheManager("holidays", "countries");
		manager.setCaffeine(Caffeine.newBuilder()
				.maximumSize(cache.maximumSize())
				.expireAfterWrite(cache.expireAfterWrite())
				.recordStats());
		return manager;
	}

}
