// REVIEW: full code review requested

package com.example.ppps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PppsApplication {

	public static void main(String[] args) {

        SpringApplication.run(PppsApplication.class, args);
	}

}
