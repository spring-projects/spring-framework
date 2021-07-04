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

package org.springframework.test.context.env.repeatable;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} as a
 * repeatable annotation.
 *
 * <p>Same as {@link ReversedExplicitPropertiesFilesRepeatedTestPropertySourceTests},
 * but with the order of the properties files reversed.
 *
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @since 5.2
 */
@TestPropertySource("first.properties")
@TestPropertySource("second.properties")
class ExplicitPropertiesFilesRepeatedTestPropertySourceTests extends AbstractRepeatableTestPropertySourceTests {

	@Test
	void test() {
		assertEnvironmentValue("alpha", "omega");
		assertEnvironmentValue("first", "1111");
		assertEnvironmentValue("second", "2222");
	}

}
