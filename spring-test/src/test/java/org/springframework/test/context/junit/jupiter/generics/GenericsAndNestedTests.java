/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.generics;

import org.junit.jupiter.api.Nested;

import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.comics.Cat;
import org.springframework.test.context.junit.jupiter.comics.Dog;

/**
 * Integration tests that verify support for Java generics combined with {@code @Nested}
 * test classes when used with the Spring TestContext Framework and the
 * {@link SpringExtension}.
 *
 * @author Sam Brannen
 * @since 5.0.5
 */
class GenericsAndNestedTests {

	@Nested
	class CatTests extends GenericComicCharactersTests<Cat> {

		@Override
		int getExpectedNumCharacters() {
			return 2;
		}

		@Override
		String getExpectedName() {
			return "Catbert";
		}
	}

	@Nested
	class DogTests extends GenericComicCharactersTests<Dog> {

		@Override
		int getExpectedNumCharacters() {
			return 1;
		}

		@Override
		String getExpectedName() {
			return "Dogbert";
		}
	}

}
