/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.customizers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.ContextCustomizerFactories.MergeMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@ContextCustomizerFactories(factories = {FooContextCustomizerFactory.class, BarContextCustomizerFactory.class},
	mergeMode = MergeMode.REPLACE_DEFAULTS)
class ReplaceDefaultsContextCustomizerTests {

	// GlobalFruitContextCustomizerFactory is registered via spring.factories
	@Autowired(required = false)
	@Qualifier("global$fruit")
	String fruit;

	@Autowired(required = false)
	@Qualifier("foo")
	String foo;

	@Autowired(required = false)
	@Qualifier("bar")
	String bar;


	@Test
	void injectedBean() {
		// MergeMode.REPLACE_DEFAULTS overrides spring.factories lookup
		assertThat(fruit).isNull();

		// From local @ContextCustomizerFactories
		assertThat(foo).isEqualTo("bar");
		assertThat(bar).isEqualTo("baz");
	}

}
