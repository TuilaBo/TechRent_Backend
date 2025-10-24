package com.rentaltech.techrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TechRentSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechRentSystemApplication.class, args);
	}

}
