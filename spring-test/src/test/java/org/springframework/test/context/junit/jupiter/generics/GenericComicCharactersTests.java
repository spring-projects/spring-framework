/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.TestConfig;
import org.springframework.test.context.junit.jupiter.comics.Character;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests that demonstrate support for
 * Java generics in JUnit Jupiter test classes when used with the Spring TestContext
 * Framework and the {@link SpringExtension}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@SpringJUnitConfig(TestConfig.class)
abstract class GenericComicCharactersTests<T extends Character> {

	@Autowired
	T character;

	@Autowired
	List<T> characters;

	@Test
	void autowiredFields() {
		assertThat(this.character).as("Character should have been @Autowired by Spring").isNotNull();
		assertThat(this.character).as("character's name").extracting(Character::getName).isEqualTo(getExpectedName());
		assertThat(this.characters).as("Number of characters in context").hasSize(getExpectedNumCharacters());
	}

	@Test
	void autowiredParameterByTypeForSingleGenericBean(@Autowired T character) {
		assertThat(character).as("Character should have been @Autowired by Spring").isNotNull();
		assertThat(this.character).as("character's name").extracting(Character::getName).isEqualTo(getExpectedName());
	}

	abstract int getExpectedNumCharacters();

	abstract String getExpectedName();

}
