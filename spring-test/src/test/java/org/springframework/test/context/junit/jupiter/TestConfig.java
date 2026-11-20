/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.comics.Cat;
import org.springframework.test.context.junit.jupiter.comics.Dog;
import org.springframework.test.context.junit.jupiter.comics.Person;

/**
 * Demo config for tests.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@Configuration
public class TestConfig {

	@Bean
	Person dilbert() {
		return new Person("Dilbert");
	}

	@Bean
	Person wally() {
		return new Person("Wally");
	}

	@Bean
	Dog dogbert() {
		return new Dog("Dogbert");
	}

	@Primary
	@Bean
	Cat catbert() {
		return new Cat("Catbert");
	}

	@Bean
	Cat garfield() {
		return new Cat("Garfield");
	}

}
