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
 * Same as {@link ExplicitPropertiesFilesRepeatedTestPropertySourceTests}, but
 * with the order of the properties files reversed.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@TestPropertySource("second.properties")
@TestPropertySource("first.properties")
class ReversedExplicitPropertiesFilesRepeatedTestPropertySourceTests extends AbstractRepeatableTestPropertySourceTests {

	@Test
	void test() {
		assertEnvironmentValue("alpha", "beta");
		assertEnvironmentValue("first", "1111");
		assertEnvironmentValue("second", "1111");
	}

}
