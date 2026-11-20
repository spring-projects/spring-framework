/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.test.context.initializers.FooBarAliasInitializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in conjunction with annotation-driven
 * configuration in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig(classes = { GlobalConfig.class, DevProfileConfig.class }, initializers = FooBarAliasInitializer.class)
class SingleInitializerAnnotationConfigTests {

	@Autowired
	protected String foo;

	@Autowired(required = false)
	@Qualifier("bar")
	protected String bar;

	@Autowired
	protected String baz;


	@Test
	void activeBeans() {
		assertThat(foo).isEqualTo("foo");
		assertThat(bar).isEqualTo("foo");
		assertThat(baz).isEqualTo("global config");
	}

}
