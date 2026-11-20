/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.rules;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Subclass #2 of {@link BaseAppCtxRuleTests}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@ContextConfiguration
public class Subclass2AppCtxRuleTests extends BaseAppCtxRuleTests {

	@Autowired
	private String baz;


	@Test
	public void baz() {
		assertThat(baz).isEqualTo("baz");
	}


	@Configuration
	static class Config {

		@Bean
		public String baz() {
			return "baz";
		}
	}
}
