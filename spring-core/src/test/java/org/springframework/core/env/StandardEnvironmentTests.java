/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.env;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.SpringProperties;
import org.springframework.core.testfixture.env.MockPropertySource;
import org.springframework.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;
import static org.springframework.core.env.AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME;
import static org.springframework.core.env.AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME;

/**
 * Tests for {@link StandardEnvironment}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class StandardEnvironmentTests {

	private static final String ALLOWED_PROPERTY_NAME = "theanswer";
	private static final String ALLOWED_PROPERTY_VALUE = "42";

	private static final String DISALLOWED_PROPERTY_NAME = "verboten";
	private static final String DISALLOWED_PROPERTY_VALUE = "secret";

	private static final String STRING_PROPERTY_NAME = "stringPropName";
	private static final String STRING_PROPERTY_VALUE = "stringPropValue";
	private static final Object NON_STRING_PROPERTY_NAME = new Object();
	private static final Object NON_STRING_PROPERTY_VALUE = new Object();

	private final ConfigurableEnvironment environment = new StandardEnvironment();


	@Test
	void merge() {
		ConfigurableEnvironment child = new StandardEnvironment();
		child.setActiveProfiles("c1", "c2");
		child.getPropertySources().addLast(
				new MockPropertySource("childMock")
						.withProperty("childKey", "childVal")
						.withProperty("bothKey", "childBothVal"));

		ConfigurableEnvironment parent = new StandardEnvironment();
		parent.setActiveProfiles("p1", "p2");
		parent.getPropertySources().addLast(
				new MockPropertySource("parentMock")
						.withProperty("parentKey", "parentVal")
						.withProperty("bothKey", "parentBothVal"));

		assertThat(child.getProperty("childKey")).isEqualTo("childVal");
		assertThat(child.getProperty("parentKey")).isNull();
		assertThat(child.getProperty("bothKey")).isEqualTo("childBothVal");

		assertThat(parent.getProperty("childKey")).isNull();
		assertThat(parent.getProperty("parentKey")).isEqualTo("parentVal");
		assertThat(parent.getProperty("bothKey")).isEqualTo("parentBothVal");

		assertThat(child.getActiveProfiles()).containsExactly("c1", "c2");
		assertThat(parent.getActiveProfiles()).containsExactly("p1", "p2");

		child.merge(parent);

		assertThat(child.getProperty("childKey")).isEqualTo("childVal");
		assertThat(child.getProperty("parentKey")).isEqualTo("parentVal");
		assertThat(child.getProperty("bothKey")).isEqualTo("childBothVal");

		assertThat(parent.getProperty("childKey")).isNull();
		assertThat(parent.getProperty("parentKey")).isEqualTo("parentVal");
		assertThat(parent.getProperty("bothKey")).isEqualTo("parentBothVal");

		assertThat(child.getActiveProfiles()).containsExactly("c1", "c2", "p1", "p2");
		assertThat(parent.getActiveProfiles()).containsExactly("p1", "p2");
	}

	@Test
	void propertySourceOrder() {
		ConfigurableEnvironment env = new StandardEnvironment();
		MutablePropertySources sources = env.getPropertySources();
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME))).isEqualTo(0);
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))).isEqualTo(1);
		assertThat(sources).hasSize(2);
	}

	@Test
	void propertySourceTypes() {
		ConfigurableEnvironment env = new StandardEnvironment();
		MutablePropertySources sources = env.getPropertySources();
		assertThat(sources.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)).isInstanceOf(SystemEnvironmentPropertySource.class);
	}

	@Test
	void activeProfilesIsEmptyByDefault() {
		assertThat(environment.getActiveProfiles()).isEmpty();
	}

	@Test
	void defaultProfilesContainsDefaultProfileByDefault() {
		assertThat(environment.getDefaultProfiles()).containsExactly("default");
	}

	@Test
	void setActiveProfiles() {
		environment.setActiveProfiles("local", "embedded");
		String[] activeProfiles = environment.getActiveProfiles();
		assertThat(activeProfiles).containsExactly("local", "embedded");
	}

	@Test
	void setActiveProfiles_withNullProfileArray() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles((String[]) null));
	}

	@Test
	void setActiveProfiles_withNullProfile() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles((String) null));
	}

	@Test
	void setActiveProfiles_withEmptyProfile() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles(""));
	}

	@Test
	void setActiveProfiles_withNotOperator() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles("p1", "!p2"));
	}

	@Test
	void setDefaultProfiles_withNullProfileArray() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles((String[]) null));
	}

	@Test
	void setDefaultProfiles_withNullProfile() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles((String) null));
	}

	@Test
	void setDefaultProfiles_withEmptyProfile() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles(""));
	}

	@Test
	void setDefaultProfiles_withNotOperator() {
		assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles("d1", "!d2"));
	}

	@Test
	void addActiveProfile() {
		assertThat(environment.getActiveProfiles()).isEmpty();
		environment.setActiveProfiles("local", "embedded");
		assertThat(environment.getActiveProfiles()).containsExactly("local", "embedded");
		environment.addActiveProfile("p1");
		assertThat(environment.getActiveProfiles()).containsExactly("local", "embedded", "p1");
		environment.addActiveProfile("p2");
		environment.addActiveProfile("p3");
		assertThat(environment.getActiveProfiles()).containsExactly("local", "embedded", "p1", "p2", "p3");
	}

	@Test
	void addActiveProfile_whenActiveProfilesPropertyIsAlreadySet() {
		ConfigurableEnvironment env = new StandardEnvironment();
		assertThat(env.getProperty(ACTIVE_PROFILES_PROPERTY_NAME)).isNull();
		env.getPropertySources().addFirst(new MockPropertySource().withProperty(ACTIVE_PROFILES_PROPERTY_NAME, "p1"));
		assertThat(env.getProperty(ACTIVE_PROFILES_PROPERTY_NAME)).isEqualTo("p1");
		env.addActiveProfile("p2");
		assertThat(env.getActiveProfiles()).containsExactly("p1", "p2");
	}

	@Test
	void reservedDefaultProfile() {
		assertThat(environment.getDefaultProfiles()).containsExactly(RESERVED_DEFAULT_PROFILE_NAME);
		try {
			System.setProperty(DEFAULT_PROFILES_PROPERTY_NAME, "d0");
			assertThat(environment.getDefaultProfiles()).containsExactly("d0");
			environment.setDefaultProfiles("d1", "d2");
			assertThat(environment.getDefaultProfiles()).containsExactly("d1","d2");
		}
		finally {
			System.clearProperty(DEFAULT_PROFILES_PROPERTY_NAME);
		}
	}

	@Test
	void defaultProfileWithCircularPlaceholder() {
		try {
			System.setProperty(DEFAULT_PROFILES_PROPERTY_NAME, "${spring.profiles.default}");
			assertThatExceptionOfType(PlaceholderResolutionException.class)
				.isThrownBy(environment::getDefaultProfiles)
				.withMessageContaining("Circular placeholder reference 'spring.profiles.default'");
		}
		finally {
			System.clearProperty(DEFAULT_PROFILES_PROPERTY_NAME);
		}
	}

	@Test
	void getDefaultProfiles() {
		assertThat(environment.getDefaultProfiles()).containsExactly(RESERVED_DEFAULT_PROFILE_NAME);
		environment.getPropertySources().addFirst(new MockPropertySource().withProperty(DEFAULT_PROFILES_PROPERTY_NAME, "pd1"));
		assertThat(environment.getDefaultProfiles()).containsExactly("pd1");
	}

	@Test
	void setDefaultProfiles() {
		environment.setDefaultProfiles();
		assertThat(environment.getDefaultProfiles()).isEmpty();
		environment.setDefaultProfiles("pd1");
		assertThat(environment.getDefaultProfiles()).containsExactly("pd1");
		environment.setDefaultProfiles("pd2", "pd3");
		assertThat(environment.getDefaultProfiles()).containsExactly("pd2", "pd3");
	}

	@Test
	void environmentSubclass_withCustomProfileValidation() {
		ConfigurableEnvironment env = new AbstractEnvironment() {
			@Override
			protected void validateProfile(String profile) {
				super.validateProfile(profile);
				if (profile.contains("-")) {
					throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not contain dash character");
				}
			}
		};

		env.addActiveProfile("validProfile"); // succeeds

		assertThatIllegalArgumentException()
			.isThrownBy(() -> env.addActiveProfile("invalid-profile"))
			.withMessage("Invalid profile [invalid-profile]: must not contain dash character");
	}

	@Test
	void suppressGetenvAccessThroughSystemProperty() {
		try {
			System.setProperty("spring.getenv.ignore", "true");
			assertThat(environment.getSystemEnvironment()).isEmpty();
		}
		finally {
			System.clearProperty("spring.getenv.ignore");
		}
	}

	@Test
	void suppressGetenvAccessThroughSpringProperty() {
		try {
			SpringProperties.setProperty("spring.getenv.ignore", "true");
			assertThat(environment.getSystemEnvironment()).isEmpty();
		}
		finally {
			SpringProperties.setProperty("spring.getenv.ignore", null);
		}
	}

	@Test
	void suppressGetenvAccessThroughSpringFlag() {
		try {
			SpringProperties.setFlag("spring.getenv.ignore");
			assertThat(environment.getSystemEnvironment()).isEmpty();
		}
		finally {
			SpringProperties.setProperty("spring.getenv.ignore", null);
		}
	}

	@Test
	void getSystemProperties() {
		try {
			System.setProperty(ALLOWED_PROPERTY_NAME, ALLOWED_PROPERTY_VALUE);
			System.setProperty(DISALLOWED_PROPERTY_NAME, DISALLOWED_PROPERTY_VALUE);
			System.getProperties().put(STRING_PROPERTY_NAME, NON_STRING_PROPERTY_VALUE);
			System.getProperties().put(NON_STRING_PROPERTY_NAME, STRING_PROPERTY_VALUE);

			Map<?, ?> systemProperties = environment.getSystemProperties();
			assertThat(systemProperties).isNotNull();
			assertThat(System.getProperties()).isSameAs(systemProperties);
			assertThat(systemProperties.get(ALLOWED_PROPERTY_NAME)).isEqualTo(ALLOWED_PROPERTY_VALUE);
			assertThat(systemProperties.get(DISALLOWED_PROPERTY_NAME)).isEqualTo(DISALLOWED_PROPERTY_VALUE);
			assertThat(systemProperties.get(STRING_PROPERTY_NAME)).isEqualTo(NON_STRING_PROPERTY_VALUE);
			assertThat(systemProperties.get(NON_STRING_PROPERTY_NAME)).isEqualTo(STRING_PROPERTY_VALUE);

			PropertiesPropertySource systemPropertySource = (PropertiesPropertySource)
					environment.getPropertySources().get(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
			Set<String> expectedKeys = new HashSet<>(System.getProperties().stringPropertyNames());
			expectedKeys.add(STRING_PROPERTY_NAME);  // filtered out by stringPropertyNames due to non-String value
			assertThat(Set.of(systemPropertySource.getPropertyNames())).isEqualTo(expectedKeys);
		}
		finally {
			System.clearProperty(ALLOWED_PROPERTY_NAME);
			System.clearProperty(DISALLOWED_PROPERTY_NAME);
			System.getProperties().remove(STRING_PROPERTY_NAME);
			System.getProperties().remove(NON_STRING_PROPERTY_NAME);
		}
	}

	@Test
	void getSystemEnvironment() {
		Map<String, Object> systemEnvironment = environment.getSystemEnvironment();
		assertThat(systemEnvironment).isNotNull();
		assertThat(System.getenv()).isSameAs(systemEnvironment);
	}


	@Nested
	class GetActiveProfiles {

		@Test
		void systemPropertiesEmpty() {
			assertThat(environment.getActiveProfiles()).isEmpty();
			try {
				System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "");
				assertThat(environment.getActiveProfiles()).isEmpty();
			}
			finally {
				System.clearProperty(ACTIVE_PROFILES_PROPERTY_NAME);
			}
		}

		@Test
		void fromSystemProperties() {
			try {
				System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "foo");
				assertThat(environment.getActiveProfiles()).containsExactly("foo");
			}
			finally {
				System.clearProperty(ACTIVE_PROFILES_PROPERTY_NAME);
			}
		}

		@Test
		void fromSystemProperties_withMultipleProfiles() {
			try {
				System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "foo,bar");
				assertThat(environment.getActiveProfiles()).containsExactly("foo", "bar");
			}
			finally {
				System.clearProperty(ACTIVE_PROFILES_PROPERTY_NAME);
			}
		}

		@Test
		void fromSystemProperties_withMultipleProfiles_withWhitespace() {
			try {
				System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, " bar , baz "); // notice whitespace
				assertThat(environment.getActiveProfiles()).containsExactly("bar", "baz");
			}
			finally {
				System.clearProperty(ACTIVE_PROFILES_PROPERTY_NAME);
			}
		}
	}


	@Nested
	class AcceptsProfilesTests {

		@Test
		@SuppressWarnings("deprecation")
		void withEmptyArgumentList() {
			assertThatIllegalArgumentException().isThrownBy(environment::acceptsProfiles);
		}

		@Test
		@SuppressWarnings("deprecation")
		void withNullArgumentList() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles((String[]) null));
		}

		@Test
		@SuppressWarnings("deprecation")
		void withNullArgument() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles((String) null));
		}

		@Test
		@SuppressWarnings("deprecation")
		void withEmptyArgument() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles(""));
		}

		@Test
		@SuppressWarnings("deprecation")
		void activeProfileSetProgrammatically() {
			assertThat(environment.acceptsProfiles("p1", "p2")).isFalse();
			environment.setActiveProfiles("p1");
			assertThat(environment.acceptsProfiles("p1", "p2")).isTrue();
			environment.setActiveProfiles("p2");
			assertThat(environment.acceptsProfiles("p1", "p2")).isTrue();
			environment.setActiveProfiles("p1", "p2");
			assertThat(environment.acceptsProfiles("p1", "p2")).isTrue();
		}

		@Test
		@SuppressWarnings("deprecation")
		void activeProfileSetViaProperty() {
			assertThat(environment.acceptsProfiles("p1")).isFalse();
			environment.getPropertySources().addFirst(new MockPropertySource().withProperty(ACTIVE_PROFILES_PROPERTY_NAME, "p1"));
			assertThat(environment.acceptsProfiles("p1")).isTrue();
		}

		@Test
		@SuppressWarnings("deprecation")
		void defaultProfile() {
			assertThat(environment.acceptsProfiles("pd")).isFalse();
			environment.setDefaultProfiles("pd");
			assertThat(environment.acceptsProfiles("pd")).isTrue();
			environment.setActiveProfiles("p1");
			assertThat(environment.acceptsProfiles("pd")).isFalse();
			assertThat(environment.acceptsProfiles("p1")).isTrue();
		}

		@Test
		@SuppressWarnings("deprecation")
		void withNotOperator() {
			assertThat(environment.acceptsProfiles("p1")).isFalse();
			assertThat(environment.acceptsProfiles("!p1")).isTrue();
			environment.addActiveProfile("p1");
			assertThat(environment.acceptsProfiles("p1")).isTrue();
			assertThat(environment.acceptsProfiles("!p1")).isFalse();
		}

		@Test
		@SuppressWarnings("deprecation")
		void withInvalidNotOperator() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles("p1", "!"));
		}

		@Test
		void withProfileExpression() {
			assertThat(environment.acceptsProfiles(Profiles.of("p1 & p2"))).isFalse();
			environment.addActiveProfile("p1");
			assertThat(environment.acceptsProfiles(Profiles.of("p1 & p2"))).isFalse();
			environment.addActiveProfile("p2");
			assertThat(environment.acceptsProfiles(Profiles.of("p1 & p2"))).isTrue();
		}
	}


	@Nested
	class MatchesProfilesTests {

		@Test
		void withEmptyArgumentList() {
			assertThatIllegalArgumentException().isThrownBy(environment::matchesProfiles);
		}

		@Test
		void withNullArgumentList() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles((String[]) null));
		}

		@Test
		void withNullArgument() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles((String) null));
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles("p1", null));
		}

		@Test
		void withEmptyArgument() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles(""));
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles("p1", ""));
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles("p1", "      "));
		}

		@Test
		void withInvalidNotOperator() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles("p1", "!"));
		}

		@Test
		void withInvalidCompoundExpressionGrouping() {
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles("p1 | p2 & p3"));
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles("p1 & p2 | p3"));
			assertThatIllegalArgumentException().isThrownBy(() -> environment.matchesProfiles("p1 & (p2 | p3) | p4"));
		}

		@Test
		void activeProfileSetProgrammatically() {
			assertThat(environment.matchesProfiles("p1", "p2")).isFalse();

			environment.setActiveProfiles("p1");
			assertThat(environment.matchesProfiles("p1", "p2")).isTrue();

			environment.setActiveProfiles("p2");
			assertThat(environment.matchesProfiles("p1", "p2")).isTrue();

			environment.setActiveProfiles("p1", "p2");
			assertThat(environment.matchesProfiles("p1", "p2")).isTrue();
		}

		@Test
		void activeProfileSetViaProperty() {
			assertThat(environment.matchesProfiles("p1")).isFalse();

			environment.getPropertySources().addFirst(new MockPropertySource().withProperty(ACTIVE_PROFILES_PROPERTY_NAME, "p1"));
			assertThat(environment.matchesProfiles("p1")).isTrue();
		}

		@Test
		void defaultProfile() {
			assertThat(environment.matchesProfiles("pd")).isFalse();

			environment.setDefaultProfiles("pd");
			assertThat(environment.matchesProfiles("pd")).isTrue();

			environment.setActiveProfiles("p1");
			assertThat(environment.matchesProfiles("pd")).isFalse();
			assertThat(environment.matchesProfiles("p1")).isTrue();
		}

		@Test
		void withNotOperator() {
			assertThat(environment.matchesProfiles("p1")).isFalse();
			assertThat(environment.matchesProfiles("!p1")).isTrue();

			environment.addActiveProfile("p1");
			assertThat(environment.matchesProfiles("p1")).isTrue();
			assertThat(environment.matchesProfiles("!p1")).isFalse();
		}

		@Test
		void withProfileExpressions() {
			assertThat(environment.matchesProfiles("p1 & p2")).isFalse();

			environment.addActiveProfile("p1");
			assertThat(environment.matchesProfiles("p1 | p2")).isTrue();
			assertThat(environment.matchesProfiles("p1 & p2")).isFalse();

			environment.addActiveProfile("p2");
			assertThat(environment.matchesProfiles("p1 & p2")).isTrue();
			assertThat(environment.matchesProfiles("p1 | p2")).isTrue();
			assertThat(environment.matchesProfiles("foo | p1", "p2")).isTrue();
			assertThat(environment.matchesProfiles("foo | p2", "p1")).isTrue();
			assertThat(environment.matchesProfiles("foo | (p2 & p1)")).isTrue();
			assertThat(environment.matchesProfiles("p2 & (foo | p1)")).isTrue();
			assertThat(environment.matchesProfiles("foo", "(p2 & p1)")).isTrue();
		}
	}

}
