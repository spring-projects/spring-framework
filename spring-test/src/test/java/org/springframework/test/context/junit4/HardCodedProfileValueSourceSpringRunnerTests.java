/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
@DisabledInAotMode("Because EnabledAndIgnoredSpringRunnerTests is disabled in AOT mode")
@SuppressWarnings("deprecation")
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
