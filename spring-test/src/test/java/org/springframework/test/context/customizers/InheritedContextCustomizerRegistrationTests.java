/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.customizers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@CustomizeWithBar
class InheritedContextCustomizerRegistrationTests extends LocalContextCustomizerRegistrationTests {

	@Autowired
	@Qualifier("bar")
	String bar;


	@Override
	@Test
	void injectedBean() {
		assertThat(fruit).isEqualTo("apple, banana, cherry");
		assertThat(foo).isEqualTo("bar");
		assertThat(bar).isEqualTo("baz");
	}

}
