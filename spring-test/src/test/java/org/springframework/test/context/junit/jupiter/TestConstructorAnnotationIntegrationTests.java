/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.comics.Dog;
import org.springframework.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

/**
 * Integration tests which demonstrate support for automatically
 * {@link Autowired @Autowired} test class constructors in conjunction with the
 * {@link TestConstructor @TestConstructor} annotation
 *
 * @author Sam Brannen
 * @since 5.2
 * @see SpringExtension
 * @see SpringJUnitJupiterAutowiredConstructorInjectionTests
 * @see SpringJUnitJupiterConstructorInjectionTests
 */
@SpringJUnitConfig(TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
@TestConstructor(autowireMode = ALL)
class TestConstructorAnnotationIntegrationTests {

	final ApplicationContext applicationContext;
	final Person dilbert;
	final Dog dog;
	final Integer enigma;


	TestConstructorAnnotationIntegrationTests(ApplicationContext applicationContext, Person dilbert, Dog dog,
			@Value("${enigma}") Integer enigma) {

		this.applicationContext = applicationContext;
		this.dilbert = dilbert;
		this.dog = dog;
		this.enigma = enigma;
	}

	@Test
	void applicationContextInjected() {
		assertThat(applicationContext).as("ApplicationContext should have been injected by Spring").isNotNull();
		assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(this.dilbert);
	}

	@Test
	void beansInjected() {
		assertThat(this.dilbert).as("Dilbert should have been @Autowired by Spring").isNotNull();
		assertThat(this.dilbert.getName()).as("Person's name").isEqualTo("Dilbert");

		assertThat(this.dog).as("Dogbert should have been @Autowired by Spring").isNotNull();
		assertThat(this.dog.getName()).as("Dog's name").isEqualTo("Dogbert");
	}

	@Test
	void propertyPlaceholderInjected() {
		assertThat(this.enigma).as("Enigma should have been injected via @Value by Spring").isNotNull();
		assertThat(this.enigma).as("enigma").isEqualTo(42);
	}

}
