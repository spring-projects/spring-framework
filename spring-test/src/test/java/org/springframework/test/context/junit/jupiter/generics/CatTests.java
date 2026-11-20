/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.generics;

import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.comics.Cat;

/**
 * Parameterized test class for integration tests that demonstrate support for
 * Java generics in JUnit Jupiter test classes when used with the Spring TestContext
 * Framework and the {@link SpringExtension}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
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
