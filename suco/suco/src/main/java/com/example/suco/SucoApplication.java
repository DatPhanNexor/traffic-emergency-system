
package com.example.suco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SucoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SucoApplication.class, args);
	}

}