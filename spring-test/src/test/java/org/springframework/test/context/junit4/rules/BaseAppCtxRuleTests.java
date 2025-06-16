/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests involving Spring {@code ApplicationContexts}
 * in conjunction with {@link SpringClassRule} and {@link SpringMethodRule}.
 *
 * <p>The goal of this class and its subclasses is to ensure that Rule-based
 * configuration can be inherited without requiring {@link SpringClassRule}
 * or {@link SpringMethodRule} to be redeclared on subclasses.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Subclass1AppCtxRuleTests
 * @see Subclass2AppCtxRuleTests
 */
@ContextConfiguration
@SuppressWarnings("deprecation")
public class BaseAppCtxRuleTests {

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Autowired
	private String foo;


	@Test
	public void foo() {
		assertThat(foo).isEqualTo("foo");
	}


	@Configuration
	static class Config {

		@Bean
		public String foo() {
			return "foo";
		}
	}
}
