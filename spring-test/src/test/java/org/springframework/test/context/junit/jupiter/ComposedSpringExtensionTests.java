/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate the composability of annotations from
 * JUnit Jupiter and the Spring TestContext Framework.
 *
 * <p>Note that {@link SpringJUnitConfig @SpringJUnitConfig} is meta-annotated
 * with JUnit Jupiter's {@link ExtendWith @ExtendWith} <b>and</b> Spring's
 * {@link ContextConfiguration @ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see SpringJUnitConfig
 * @see SpringExtensionTests
 */
@SpringJUnitConfig(TestConfig.class)
@DisplayName("@SpringJUnitConfig Tests")
class ComposedSpringExtensionTests {

	@Autowired
	Person dilbert;

	@Autowired
	List<Person> people;

	@Test
	@DisplayName("ApplicationContext injected into method")
	void applicationContextInjected(ApplicationContext applicationContext) {
		assertThat(applicationContext).as("ApplicationContext should have been injected into method by Spring").isNotNull();
		assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(dilbert);
	}

	@Test
	@DisplayName("Spring @Beans injected into fields")
	void springBeansInjected() {
		assertThat(dilbert).as("Person should have been @Autowired by Spring").isNotNull();
		assertThat(dilbert.getName()).as("Person's name").isEqualTo("Dilbert");
		assertThat(people).as("Number of Person objects in context").hasSize(2);
	}

}
