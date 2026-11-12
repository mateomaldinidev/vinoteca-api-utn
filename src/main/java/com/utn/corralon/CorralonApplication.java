package com.utn.corralon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CorralonApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorralonApplication.class, args);
	}

}
