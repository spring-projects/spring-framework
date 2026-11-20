/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.customizers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextCustomizerFactories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@ContextCustomizerFactories(factories = {BarContextCustomizerFactory.class, EnigmaContextCustomizerFactory.class},
		inheritFactories = false)
class OverriddenContextCustomizerRegistrationTests extends LocalContextCustomizerRegistrationTests {

	@Autowired
	@Qualifier("bar")
	String bar;

	@Autowired
	Integer enigma;


	@Override
	@Test
	void injectedBean() {
		// globally registered via spring.factories
		assertThat(fruit).isEqualTo("apple, banana, cherry");

		// Overridden by this subclass (inheritFactories = false)
		assertThat(foo).isNull();

		// Local to this subclass
		assertThat(bar).isEqualTo("baz");
		assertThat(enigma).isEqualTo(42);
	}

}
