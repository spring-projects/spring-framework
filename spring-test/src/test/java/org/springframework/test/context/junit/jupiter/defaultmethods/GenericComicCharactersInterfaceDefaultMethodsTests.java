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
