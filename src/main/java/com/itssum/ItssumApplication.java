package com.itssum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ItssumApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItssumApplication.class, args);
	}

}
