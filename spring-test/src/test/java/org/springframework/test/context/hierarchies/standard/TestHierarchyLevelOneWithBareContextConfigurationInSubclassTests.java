/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hierarchies.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy(@ContextConfiguration)
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class TestHierarchyLevelOneWithBareContextConfigurationInSubclassTests {

	@Configuration
	static class Config {

		@Bean
		String foo() {
			return "foo-level-1";
		}

		@Bean
		String bar() {
			return "bar";
		}
	}


	@Autowired
	private String foo;

	@Autowired
	private String bar;

	@Autowired
	private ApplicationContext context;


	@Test
	void loadContextHierarchy() {
		assertThat(context).as("child ApplicationContext").isNotNull();
		assertThat(context.getParent()).as("parent ApplicationContext").isNull();
		assertThat(foo).isEqualTo("foo-level-1");
		assertThat(bar).isEqualTo("bar");
	}

}
