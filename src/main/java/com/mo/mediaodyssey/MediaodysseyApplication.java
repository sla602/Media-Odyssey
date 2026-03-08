package com.mo.mediaodyssey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MediaodysseyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaodysseyApplication.class, args);
	}

}
