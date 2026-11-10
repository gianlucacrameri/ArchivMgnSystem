package com.archiv.ArchivMgnSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArchivMgnSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArchivMgnSystemApplication.class, args);
	}

}
