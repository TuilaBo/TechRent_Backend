package com.rentaltech.techrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TechRentSystemApplication {

    public static void main(String[] args) {
        ZoneId targetZone = ZoneId.of("Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone(targetZone));
        System.out.println("Application default timezone: " + ZoneId.systemDefault());
        SpringApplication.run(TechRentSystemApplication.class, args);
    }

}