package fr.nexusproject.vault_game_api;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class VaultGameApiApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(VaultGameApiApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(VaultGameApiApplication.class, args);
	}

	@Bean
	CommandLineRunner logActiveProfiles(Environment environment) {
		return args -> {
			String[] activeProfiles = environment.getActiveProfiles();
			String[] defaultProfiles = environment.getDefaultProfiles();
			LOGGER.info("Active Spring profiles: {}", Arrays.toString(activeProfiles));
			LOGGER.info("Default Spring profiles: {}", Arrays.toString(defaultProfiles));

			boolean devProfileEffective = environment.acceptsProfiles(Profiles.of("dev"));
			LOGGER.info("Dev profile effective: {}", devProfileEffective);
		};
	}

}
