/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.customizers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which verifies support for {@link ContextCustomizerFactory}
 * and {@link ContextCustomizer} when a custom factory is registered declaratively
 * via {@link ContextCustomizerFactories @ContextCustomizerFactories}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@CustomizeWithFruit
@CustomizeWithFoo
@CustomizeWithBar
class ContextCustomizerDeclarativeRegistrationComposedAnnotationTests {

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
		// registered globally via spring.factories
		assertThat(fruit).isEqualTo("apple, banana, cherry");

		// From local @ContextCustomizerFactories
		assertThat(foo).isEqualTo("bar");

		// @ContextCustomizerFactories is not currently supported as a repeatable annotation.
		assertThat(bar).isNull();
	}

}
