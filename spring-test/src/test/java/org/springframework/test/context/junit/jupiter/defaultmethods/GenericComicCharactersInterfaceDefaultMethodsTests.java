/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.defaultmethods;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.TestConfig;
import org.springframework.test.context.junit.jupiter.comics.Character;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Interface for integration tests that demonstrate support for interface default
 * methods and Java generics in JUnit Jupiter test classes when used with the Spring
 * TestContext Framework and the {@link SpringExtension}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@SpringJUnitConfig(TestConfig.class)
interface GenericComicCharactersInterfaceDefaultMethodsTests<C extends Character> {

	@Test
	default void autowiredParameterWithParameterizedList(@Autowired List<C> characters) {
		assertThat(characters).as("Number of characters in context").hasSize(getExpectedNumCharacters());
	}

	@Test
	default void autowiredParameterWithGenericBean(@Autowired C character) {
		assertThat(character).as("Character should have been @Autowired by Spring").isNotNull();
		assertThat(character).as("character's name").extracting(Character::getName).isEqualTo(getExpectedName());
	}

	int getExpectedNumCharacters();

	String getExpectedName();

}
