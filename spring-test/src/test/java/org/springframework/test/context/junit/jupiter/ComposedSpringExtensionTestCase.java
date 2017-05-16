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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.SpringJUnitJupiterTestSuite;
import org.springframework.test.context.junit.jupiter.comics.Person;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests which demonstrate the composability of annotations from
 * JUnit Jupiter and the Spring TestContext Framework.
 *
 * <p>Note that {@link SpringJUnitConfig @SpringJUnitConfig} is meta-annotated
 * with JUnit Jupiter's {@link ExtendWith @ExtendWith} <b>and</b> Spring's
 * {@link ContextConfiguration @ContextConfiguration}.
 *
 * <p>To run these tests in an IDE, simply run {@link SpringJUnitJupiterTestSuite}
 * as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see SpringJUnitConfig
 * @see SpringExtensionTestCase
 */
@SpringJUnitConfig(TestConfig.class)
@DisplayName("@SpringJUnitConfig Tests")
class ComposedSpringExtensionTestCase {

	@Autowired
	Person dilbert;

	@Autowired
	List<Person> people;

	@Test
	@DisplayName("ApplicationContext injected into method")
	void applicationContextInjected(ApplicationContext applicationContext) {
		assertNotNull(applicationContext, "ApplicationContext should have been injected into method by Spring");
		assertEquals(dilbert, applicationContext.getBean("dilbert", Person.class));
	}

	@Test
	@DisplayName("Spring @Beans injected into fields")
	void springBeansInjected() {
		assertNotNull(dilbert, "Person should have been @Autowired by Spring");
		assertEquals("Dilbert", dilbert.getName(), "Person's name");
		assertEquals(2, people.size(), "Number of Person objects in context");
	}

}
