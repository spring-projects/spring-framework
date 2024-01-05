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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests covering the extensibility of {@link AbstractEnvironment}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
class CustomEnvironmentTests {

	private static final String DEFAULT_PROFILE = AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME;


	@Test
	void control() {
		Environment env = new AbstractEnvironment() { };
		assertThat(env.matchesProfiles(DEFAULT_PROFILE)).isTrue();
	}

	@Test
	void withNoReservedDefaultProfile() {
		class CustomEnvironment extends AbstractEnvironment {
			@Override
			protected Set<String> getReservedDefaultProfiles() {
				return Set.of();
			}
		}

		Environment env = new CustomEnvironment();
		assertThat(env.matchesProfiles(DEFAULT_PROFILE)).isFalse();
	}

	@Test
	void withSingleCustomReservedDefaultProfile() {
		class CustomEnvironment extends AbstractEnvironment {
			@Override
			protected Set<String> getReservedDefaultProfiles() {
				return Set.of("rd1");
			}
		}

		Environment env = new CustomEnvironment();
		assertThat(env.matchesProfiles(DEFAULT_PROFILE)).isFalse();
		assertThat(env.matchesProfiles("rd1")).isTrue();
	}

	@Test
	void withMultiCustomReservedDefaultProfile() {
		class CustomEnvironment extends AbstractEnvironment {
			@Override
			protected Set<String> getReservedDefaultProfiles() {
				return Set.of("rd1", "rd2");
			}
		}

		ConfigurableEnvironment env = new CustomEnvironment();
		assertThat(env.matchesProfiles(DEFAULT_PROFILE)).isFalse();
		assertThat(env.matchesProfiles("rd1 | rd2")).isTrue();

		// finally, issue additional assertions to cover all combinations of calling these
		// methods, however unlikely.
		env.setDefaultProfiles("d1");
		assertThat(env.matchesProfiles("rd1 | rd2")).isFalse();
		assertThat(env.matchesProfiles("d1")).isTrue();

		env.setActiveProfiles("a1", "a2");
		assertThat(env.matchesProfiles("d1")).isFalse();
		assertThat(env.matchesProfiles("a1 | a2")).isTrue();

		env.setActiveProfiles();
		assertThat(env.matchesProfiles("d1")).isTrue();
		assertThat(env.matchesProfiles("a1 | a2")).isFalse();

		env.setDefaultProfiles();
		assertThat(env.matchesProfiles(DEFAULT_PROFILE)).isFalse();
		assertThat(env.matchesProfiles("rd1 | rd2")).isFalse();
		assertThat(env.matchesProfiles("d1")).isFalse();
		assertThat(env.matchesProfiles("a1 | a2")).isFalse();
	}

	@Test
	void withNoProfileProperties() {
		ConfigurableEnvironment env = new AbstractEnvironment() {
			@Override
			@Nullable
			protected String doGetActiveProfilesProperty() {
				return null;
			}
			@Override
			@Nullable
			protected String doGetDefaultProfilesProperty() {
				return null;
			}
		};
		Map<String, Object> values = new LinkedHashMap<>();
		values.put(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "a,b,c");
		values.put(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, "d,e,f");
		PropertySource<?> propertySource = new MapPropertySource("test", values);
		env.getPropertySources().addFirst(propertySource);
		assertThat(env.getActiveProfiles()).isEmpty();
		assertThat(env.getDefaultProfiles()).containsExactly(DEFAULT_PROFILE);
	}

	@Test
	void withCustomMutablePropertySources() {
		class CustomMutablePropertySources extends MutablePropertySources {}
		MutablePropertySources propertySources = new CustomMutablePropertySources();
		ConfigurableEnvironment env = new AbstractEnvironment(propertySources) {};
		assertThat(env.getPropertySources()).isInstanceOf(CustomMutablePropertySources.class);
	}

	@Test
	void withCustomPropertyResolver() {
		class CustomPropertySourcesPropertyResolver extends PropertySourcesPropertyResolver {
			public CustomPropertySourcesPropertyResolver(PropertySources propertySources) {
				super(propertySources);
			}
			@Override
			@Nullable
			public String getProperty(String key) {
				return super.getProperty(key) + "-test";
			}
		}

		ConfigurableEnvironment env = new AbstractEnvironment() {
			@Override
			protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
				return new CustomPropertySourcesPropertyResolver(propertySources);
			}
		};

		Map<String, Object> values = new LinkedHashMap<>();
		values.put("spring", "framework");
		PropertySource<?> propertySource = new MapPropertySource("test", values);
		env.getPropertySources().addFirst(propertySource);
		assertThat(env.getProperty("spring")).isEqualTo("framework-test");
	}

}
