/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0.3
 */
@ContextConfiguration
@ActiveProfiles("prod")
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
