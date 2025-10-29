package org.ulinda;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.ulinda.services.StartupService;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class UlindaApplication implements CommandLineRunner {

    @Autowired
    private StartupService startupService;

	public static void main(String[] args) {
		SpringApplication.run(UlindaApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
        startupService.runStartup();
        log.info("Loading Demo Data...");
        startupService.loadDemoData();
        log.info("Completed Starting Up Service");
    }
}
