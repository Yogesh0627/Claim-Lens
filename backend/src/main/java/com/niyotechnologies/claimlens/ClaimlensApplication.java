package com.niyotechnologies.claimlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ClaimlensApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClaimlensApplication.class, args);
	}

}
