/*
 * Copyright 2002-2022 the original author or authors.
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
import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.comics.Dog;
import org.springframework.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate support for autowiring individual
 * parameters in test class constructors using {@link Autowired @Autowired}
 * and {@link Value @Value} with the Spring TestContext Framework and JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see SpringJUnitJupiterAutowiredConstructorInjectionTests
 */
@SpringJUnitConfig(TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class SpringJUnitJupiterConstructorInjectionTests {

	final ApplicationContext applicationContext;
	final Person dilbert;
	final Dog dog;
	final Integer enigma;
	final TestInfo testInfo;

	SpringJUnitJupiterConstructorInjectionTests(ApplicationContext applicationContext, @Autowired Person dilbert,
			@Autowired Dog dog, @Value("${enigma}") Integer enigma, TestInfo testInfo) {

		this.applicationContext = applicationContext;
		this.dilbert = dilbert;
		this.dog = dog;
		this.enigma = enigma;
		this.testInfo = testInfo;
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

	@Test
	void testInfoInjected() {
		assertThat(this.testInfo).as("TestInfo should have been injected by JUnit").isNotNull();
	}

}
