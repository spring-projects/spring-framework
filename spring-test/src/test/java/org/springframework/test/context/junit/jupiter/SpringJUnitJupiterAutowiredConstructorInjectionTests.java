/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.SpringJUnitJupiterTestSuite;
import org.springframework.test.context.junit.jupiter.comics.Dog;
import org.springframework.test.context.junit.jupiter.comics.Person;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests which demonstrate support for {@link Autowired @Autowired}
 * test class constructors with the Spring TestContext Framework and JUnit Jupiter.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see SpringJUnitJupiterConstructorInjectionTests
 */
@SpringJUnitConfig(TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class SpringJUnitJupiterAutowiredConstructorInjectionTests {

	final ApplicationContext applicationContext;
	final Person dilbert;
	final Dog dog;
	final Integer enigma;

	@Autowired
	SpringJUnitJupiterAutowiredConstructorInjectionTests(ApplicationContext applicationContext, Person dilbert, Dog dog,
			@Value("${enigma}") Integer enigma) {

		this.applicationContext = applicationContext;
		this.dilbert = dilbert;
		this.dog = dog;
		this.enigma = enigma;
	}

	@Test
	void applicationContextInjected() {
		assertNotNull(applicationContext, "ApplicationContext should have been injected by Spring");
		assertEquals(this.dilbert, applicationContext.getBean("dilbert", Person.class));
	}

	@Test
	void beansInjected() {
		assertNotNull(this.dilbert, "Dilbert should have been @Autowired by Spring");
		assertEquals("Dilbert", this.dilbert.getName(), "Person's name");

		assertNotNull(this.dog, "Dogbert should have been @Autowired by Spring");
		assertEquals("Dogbert", this.dog.getName(), "Dog's name");
	}

	@Test
	void propertyPlaceholderInjected() {
		assertNotNull(this.enigma, "Enigma should have been injected via @Value by Spring");
		assertEquals(Integer.valueOf(42), this.enigma, "enigma");
	}

}
