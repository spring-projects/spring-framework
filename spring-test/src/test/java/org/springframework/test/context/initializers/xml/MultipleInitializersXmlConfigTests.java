/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.test.context.initializers.DevProfileInitializer;
import org.springframework.test.context.initializers.FooBarAliasInitializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in conjunction with XML configuration files
 * in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig(initializers = { FooBarAliasInitializer.class, DevProfileInitializer.class })
class MultipleInitializersXmlConfigTests {

	@Autowired
	private String foo, bar, baz;


	@Test
	void activeBeans() {
		assertThat(foo).isEqualTo("foo");
		assertThat(bar).isEqualTo("foo");
		assertThat(baz).isEqualTo("dev profile config");
	}

}
