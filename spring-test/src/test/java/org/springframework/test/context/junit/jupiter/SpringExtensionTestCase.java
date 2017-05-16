/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests which demonstrate that the Spring TestContext Framework can
 * be used with JUnit Jupiter via the {@link SpringExtension}.
 *
 * <p>To run these tests in an IDE, simply run {@link SpringJUnitJupiterTestSuite}
 * as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see ComposedSpringExtensionTestCase
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class SpringExtensionTestCase {

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
		assertNotNull(applicationContext, "ApplicationContext should have been injected by Spring");
		assertEquals(this.dilbert, applicationContext.getBean("dilbert", Person.class));
	}

	@Test
	void genericApplicationContextInjectedIntoMethod(GenericApplicationContext applicationContext) {
		assertNotNull(applicationContext, "GenericApplicationContext should have been injected by Spring");
		assertEquals(this.dilbert, applicationContext.getBean("dilbert", Person.class));
	}

	@Test
	void autowiredFields() {
		assertNotNull(this.dilbert, "Dilbert should have been @Autowired by Spring");
		assertEquals("Dilbert", this.dilbert.getName(), "Person's name");
		assertEquals(2, this.people.size(), "Number of people in context");

		assertNotNull(this.dog, "Dogbert should have been @Autowired by Spring");
		assertEquals("Dogbert", this.dog.getName(), "Dog's name");

		assertNotNull(this.cat, "Catbert should have been @Autowired by Spring as the @Primary cat");
		assertEquals("Catbert", this.cat.getName(), "Primary cat's name");
		assertEquals(2, this.cats.size(), "Number of cats in context");

		assertNotNull(this.enigma, "Enigma should have been injected via @Value by Spring");
		assertEquals(new Integer(42), this.enigma, "enigma");
	}

	@Test
	void autowiredParameterByTypeForSingleBean(@Autowired Dog dog) {
		assertNotNull(dog, "Dogbert should have been @Autowired by Spring");
		assertEquals("Dogbert", dog.getName(), "Dog's name");
	}

	@Test
	void autowiredParameterByTypeForPrimaryBean(@Autowired Cat primaryCat) {
		assertNotNull(primaryCat, "Primary cat should have been @Autowired by Spring");
		assertEquals("Catbert", primaryCat.getName(), "Primary cat's name");
	}

	@Test
	void autowiredParameterWithExplicitQualifier(@Qualifier("wally") Person person) {
		assertNotNull(person, "Wally should have been @Autowired by Spring");
		assertEquals("Wally", person.getName(), "Person's name");
	}

	/**
	 * NOTE: Test code must be compiled with "-g" (debug symbols) or "-parameters" in order
	 * for the parameter name to be used as the qualifier; otherwise, use
	 * {@code @Qualifier("wally")}.
	 */
	@Test
	void autowiredParameterWithImplicitQualifierBasedOnParameterName(@Autowired Person wally) {
		assertNotNull(wally, "Wally should have been @Autowired by Spring");
		assertEquals("Wally", wally.getName(), "Person's name");
	}

	@Test
	void autowiredParameterAsJavaUtilOptional(@Autowired Optional<Dog> dog) {
		assertNotNull(dog, "Optional dog should have been @Autowired by Spring");
		assertTrue(dog.isPresent(), "Value of Optional should be 'present'");
		assertEquals("Dogbert", dog.get().getName(), "Dog's name");
	}

	@Test
	void autowiredParameterThatDoesNotExistAsJavaUtilOptional(@Autowired Optional<Number> number) {
		assertNotNull(number, "Optional number should have been @Autowired by Spring");
		assertFalse(number.isPresent(), "Value of Optional number should not be 'present'");
	}

	@Test
	void autowiredParameterThatDoesNotExistButIsNotRequired(@Autowired(required = false) Number number) {
		assertNull(number, "Non-required number should have been @Autowired as 'null' by Spring");
	}

	@Test
	void autowiredParameterOfList(@Autowired List<Person> peopleParam) {
		assertNotNull(peopleParam, "list of people should have been @Autowired by Spring");
		assertEquals(2, peopleParam.size(), "Number of people in context");
	}

	@Test
	void valueParameterWithPrimitiveType(@Value("99") int num) {
		assertEquals(99, num);
	}

	@Test
	void valueParameterFromPropertyPlaceholder(@Value("${enigma}") Integer enigmaParam) {
		assertNotNull(enigmaParam, "Enigma should have been injected via @Value by Spring");
		assertEquals(new Integer(42), enigmaParam, "enigma");
	}

	@Test
	void valueParameterFromDefaultValueForPropertyPlaceholder(@Value("${bogus:false}") Boolean defaultValue) {
		assertNotNull(defaultValue, "Default value should have been injected via @Value by Spring");
		assertEquals(false, defaultValue, "default value");
	}

	@Test
	void valueParameterFromSpelExpression(@Value("#{@dilbert.name}") String name) {
		assertNotNull(name, "Dilbert's name should have been injected via SpEL expression in @Value by Spring");
		assertEquals("Dilbert", name, "name from SpEL expression");
	}

	@Test
	void valueParameterFromSpelExpressionWithNestedPropertyPlaceholder(@Value("#{'Hello ' + ${enigma}}") String hello) {
		assertNotNull(hello, "hello should have been injected via SpEL expression in @Value by Spring");
		assertEquals("Hello 42", hello, "hello from SpEL expression");
	}

	@Test
	void junitAndSpringMethodInjectionCombined(@Autowired Cat kittyCat, TestInfo testInfo, ApplicationContext context,
			TestReporter testReporter) {

		assertNotNull(testInfo, "TestInfo should have been injected by JUnit");
		assertNotNull(testReporter, "TestReporter should have been injected by JUnit");

		assertNotNull(context, "ApplicationContext should have been injected by Spring");
		assertNotNull(kittyCat, "Cat should have been @Autowired by Spring");
	}

}
