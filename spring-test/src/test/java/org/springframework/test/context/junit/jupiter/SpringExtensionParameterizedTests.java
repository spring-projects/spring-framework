/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.comics.Cat;
import org.springframework.test.context.junit.jupiter.comics.Dog;
import org.springframework.test.context.junit.jupiter.comics.Person;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate that the Spring TestContext Framework
 * can be used with JUnit Jupiter's {@link ParameterizedTest @ParameterizedTest}
 * support in conjunction with the {@link SpringExtension}.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see ParameterizedTest
 */
@SpringJUnitConfig(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
class SpringExtensionParameterizedTests {

	@ParameterizedTest
	@ValueSource(strings = { "Dilbert", "Wally" })
	void people(String name, @Autowired List<Person> people) {
		assertThat(people.stream().map(Person::getName).filter(name::equals)).hasSize(1);
	}

	@ParameterizedTest
	@CsvSource("dogbert, Dogbert")
	void dogs(String beanName, String dogName, ApplicationContext context) {
		assertThat(context.getBean(beanName, Dog.class)).extracting(Dog::getName).isEqualTo(dogName);
	}

	@ParameterizedTest
	@CsvSource({ "garfield, Garfield", "catbert, Catbert" })
	void cats(String beanName, String catName, ApplicationContext context) {
		assertThat(context.getBean(beanName, Cat.class)).extracting(Cat::getName).isEqualTo(catName);
	}

}
