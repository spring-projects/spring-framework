/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hierarchies.meta;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0.3
 */
@ContextConfiguration
@ActiveProfiles("prod")
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MetaHierarchyLevelTwoTests extends MetaHierarchyLevelOneTests {

	@Configuration
	@Profile("prod")
	static class Config {

		@Bean
		String bar() {
			return "Prod Bar";
		}
	}


	@Autowired
	protected ApplicationContext context;

	@Autowired
	private String bar;


	@Test
	void bar() {
		assertThat(bar).isEqualTo("Prod Bar");
	}

	@Test
	void contextHierarchy() {
		assertThat(context).as("child ApplicationContext").isNotNull();
		assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
		assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
	}

}
