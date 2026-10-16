package com.anggaarifandi.holidays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableCaching
@EnableRetry
@ConfigurationPropertiesScan
public class HolidaysApplication {

	public static void main(String[] args) {
		SpringApplication.run(HolidaysApplication.class, args);
	}

}
