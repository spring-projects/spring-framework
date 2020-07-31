/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.SpringJUnitJupiterTestSuite;
import org.springframework.test.context.junit.jupiter.comics.Cat;
import org.springframework.test.context.junit.jupiter.comics.Dog;
import org.springframework.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate that the Spring TestContext Framework can
 * be used with JUnit Jupiter via the {@link SpringExtension}.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see ComposedSpringExtensionTests
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class SpringExtensionTests {

	@Autowired
	Person dilbert;

	@Autowired
	List<Person> people;

	@Autowired
	Dog dog;

	@Autowired
	Cat cat;

	@Autowired
	List<Cat> cats;

	@Value("${enigma}")
	Integer enigma;

	@Test
	void applicationContextInjectedIntoMethod(ApplicationContext applicationContext) {
		assertThat(applicationContext).as("ApplicationContext should have been injected by Spring").isNotNull();
		assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(this.dilbert);
	}

	@Test
	void genericApplicationContextInjectedIntoMethod(GenericApplicationContext applicationContext) {
		assertThat(applicationContext).as("GenericApplicationContext should have been injected by Spring").isNotNull();
		assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(this.dilbert);
	}

	@Test
	void autowiredFields() {
		assertThat(this.dilbert).as("Dilbert should have been @Autowired by Spring").isNotNull();
		assertThat(this.dilbert.getName()).as("Person's name").isEqualTo("Dilbert");
		assertThat(this.people).as("Number of people in context").hasSize(2);

		assertThat(this.dog).as("Dogbert should have been @Autowired by Spring").isNotNull();
		assertThat(this.dog.getName()).as("Dog's name").isEqualTo("Dogbert");

		assertThat(this.cat).as("Catbert should have been @Autowired by Spring as the @Primary cat").isNotNull();
		assertThat(this.cat.getName()).as("Primary cat's name").isEqualTo("Catbert");
		assertThat(this.cats).as("Number of cats in context").hasSize(2);

		assertThat(this.enigma).as("Enigma should have been injected via @Value by Spring").isNotNull();
		assertThat(this.enigma).as("enigma").isEqualTo(42);
	}

	@Test
	void autowiredParameterByTypeForSingleBean(@Autowired Dog dog) {
		assertThat(dog).as("Dogbert should have been @Autowired by Spring").isNotNull();
		assertThat(dog.getName()).as("Dog's name").isEqualTo("Dogbert");
	}

	@Test
	void autowiredParameterByTypeForPrimaryBean(@Autowired Cat primaryCat) {
		assertThat(primaryCat).as("Primary cat should have been @Autowired by Spring").isNotNull();
		assertThat(primaryCat.getName()).as("Primary cat's name").isEqualTo("Catbert");
	}

	@Test
	void autowiredParameterWithExplicitQualifier(@Qualifier("wally") Person person) {
		assertThat(person).as("Wally should have been @Autowired by Spring").isNotNull();
		assertThat(person.getName()).as("Person's name").isEqualTo("Wally");
	}

	/**
	 * NOTE: Test code must be compiled with "-g" (debug symbols) or "-parameters" in order
	 * for the parameter name to be used as the qualifier; otherwise, use
	 * {@code @Qualifier("wally")}.
	 */
	@Test
	void autowiredParameterWithImplicitQualifierBasedOnParameterName(@Autowired Person wally) {
		assertThat(wally).as("Wally should have been @Autowired by Spring").isNotNull();
		assertThat(wally.getName()).as("Person's name").isEqualTo("Wally");
	}

	@Test
	void autowiredParameterAsJavaUtilOptional(@Autowired Optional<Dog> dog) {
		assertThat(dog).as("Optional dog should have been @Autowired by Spring").isNotNull();
		assertThat(dog).as("Value of Optional should be 'present'").isPresent();
		assertThat(dog.get().getName()).as("Dog's name").isEqualTo("Dogbert");
	}

	@Test
	void autowiredParameterThatDoesNotExistAsJavaUtilOptional(@Autowired Optional<Number> number) {
		assertThat(number).as("Optional number should have been @Autowired by Spring").isNotNull();
		assertThat(number).as("Value of Optional number should not be 'present'").isNotPresent();
	}

	@Test
	void autowiredParameterThatDoesNotExistButIsNotRequired(@Autowired(required = false) Number number) {
		assertThat(number).as("Non-required number should have been @Autowired as 'null' by Spring").isNull();
	}

	@Test
	void autowiredParameterOfList(@Autowired List<Person> peopleParam) {
		assertThat(peopleParam).as("list of people should have been @Autowired by Spring").isNotNull();
		assertThat(peopleParam).as("Number of people in context").hasSize(2);
	}

	@Test
	void valueParameterWithPrimitiveType(@Value("99") int num) {
		assertThat(num).isEqualTo(99);
	}

	@Test
	void valueParameterFromPropertyPlaceholder(@Value("${enigma}") Integer enigmaParam) {
		assertThat(enigmaParam).as("Enigma should have been injected via @Value by Spring").isNotNull();
		assertThat(enigmaParam).as("enigma").isEqualTo(42);
	}

	@Test
	void valueParameterFromDefaultValueForPropertyPlaceholder(@Value("${bogus:false}") Boolean defaultValue) {
		assertThat(defaultValue).as("Default value should have been injected via @Value by Spring").isNotNull();
		assertThat(defaultValue).as("default value").isEqualTo(false);
	}

	@Test
	void valueParameterFromSpelExpression(@Value("#{@dilbert.name}") String name) {
		assertThat(name).as("Dilbert's name should have been injected via SpEL expression in @Value by Spring").isNotNull();
		assertThat(name).as("name from SpEL expression").isEqualTo("Dilbert");
	}

	@Test
	void valueParameterFromSpelExpressionWithNestedPropertyPlaceholder(@Value("#{'Hello ' + ${enigma}}") String hello) {
		assertThat(hello).as("hello should have been injected via SpEL expression in @Value by Spring").isNotNull();
		assertThat(hello).as("hello from SpEL expression").isEqualTo("Hello 42");
	}

	@Test
	void junitAndSpringMethodInjectionCombined(@Autowired Cat kittyCat, TestInfo testInfo, ApplicationContext context,
			TestReporter testReporter) {

		assertThat(testInfo).as("TestInfo should have been injected by JUnit").isNotNull();
		assertThat(testReporter).as("TestReporter should have been injected by JUnit").isNotNull();

		assertThat(context).as("ApplicationContext should have been injected by Spring").isNotNull();
		assertThat(kittyCat).as("Cat should have been @Autowired by Spring").isNotNull();
	}

}
