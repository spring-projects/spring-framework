/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.web;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Custom <em>composed annotation</em> combining {@link WebAppConfiguration} and
 * {@link ContextConfiguration} as meta-annotations.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@WebAppConfiguration
@ContextConfiguration(classes = FooConfig.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebTestConfiguration {
}

@Configuration
class FooConfig {

	@Bean
	public String foo() {
		return "enigma";
	}
}
