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

package org.springframework.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to verify claims made in
 * <a href="https://github.com/spring-projects/spring-framework/issues/10796">gh-10796</a>.
 *
 * @author Sam Brannen
 * @author Chris Beams
 * @since 3.0
 */
@SpringJUnitConfig
class AutowiredQualifierTests {

	@Autowired
	private String foo;

	@Autowired
	@Qualifier("customFoo")
	private String customFoo;


	@Test
	void test() {
		assertThat(foo).isEqualTo("normal");
		assertThat(customFoo).isEqualTo("custom");
	}

}
