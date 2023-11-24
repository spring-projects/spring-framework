/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.junit4;

import org.junit.BeforeClass;

import org.springframework.test.annotation.ProfileValueSource;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;

/**
 * <p>
 * Verifies proper handling of JUnit's {@link org.junit.Ignore &#064;Ignore} and
 * Spring's {@link org.springframework.test.annotation.IfProfileValue
 * &#064;IfProfileValue} and {@link ProfileValueSourceConfiguration
 * &#064;ProfileValueSourceConfiguration} (with an
 * <em>explicit, custom defined {@link ProfileValueSource}</em>) annotations in
 * conjunction with the {@link SpringRunner}.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see EnabledAndIgnoredSpringRunnerTests
 */
@ProfileValueSourceConfiguration(HardCodedProfileValueSourceSpringRunnerTests.HardCodedProfileValueSource.class)
// Since EnabledAndIgnoredSpringRunnerTests is disabled in AOT mode, this test class must be also.
@DisabledInAotMode
public class HardCodedProfileValueSourceSpringRunnerTests extends EnabledAndIgnoredSpringRunnerTests {

	@BeforeClass
	public static void setProfileValue() {
		numTestsExecuted = 0;
		// Set the system property to something other than VALUE as a sanity
		// check.
		System.setProperty(NAME, "999999999999");
	}


	public static class HardCodedProfileValueSource implements ProfileValueSource {

		@Override
		public String get(final String key) {
			return (key.equals(NAME) ? VALUE : null);
		}
	}
}
