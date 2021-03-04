/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
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
 * @since 3.1
 */
class CustomEnvironmentTests {

	@Test
	void control() {
		Environment env = new AbstractEnvironment() { };
		assertThat(env.acceptsProfiles(defaultProfile())).isTrue();
	}

	@Test
	void withNoReservedDefaultProfile() {
		class CustomEnvironment extends AbstractEnvironment {
			@Override
			protected Set<String> getReservedDefaultProfiles() {
				return Collections.emptySet();
			}
		}

		Environment env = new CustomEnvironment();
		assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
	}

	@Test
	void withSingleCustomReservedDefaultProfile() {
		class CustomEnvironment extends AbstractEnvironment {
			@Override
			protected Set<String> getReservedDefaultProfiles() {
				return Collections.singleton("rd1");
			}
		}

		Environment env = new CustomEnvironment();
		assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
		assertThat(env.acceptsProfiles(Profiles.of("rd1"))).isTrue();
	}

	@Test
	void withMultiCustomReservedDefaultProfile() {
		class CustomEnvironment extends AbstractEnvironment {
			@Override
			@SuppressWarnings("serial")
			protected Set<String> getReservedDefaultProfiles() {
				return new HashSet<String>() {{
						add("rd1");
						add("rd2");
				}};
			}
		}

		ConfigurableEnvironment env = new CustomEnvironment();
		assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
		assertThat(env.acceptsProfiles(Profiles.of("rd1 | rd2"))).isTrue();

		// finally, issue additional assertions to cover all combinations of calling these
		// methods, however unlikely.
		env.setDefaultProfiles("d1");
		assertThat(env.acceptsProfiles(Profiles.of("rd1 | rd2"))).isFalse();
		assertThat(env.acceptsProfiles(Profiles.of("d1"))).isTrue();

		env.setActiveProfiles("a1", "a2");
		assertThat(env.acceptsProfiles(Profiles.of("d1"))).isFalse();
		assertThat(env.acceptsProfiles(Profiles.of("a1 | a2"))).isTrue();

		env.setActiveProfiles();
		assertThat(env.acceptsProfiles(Profiles.of("d1"))).isTrue();
		assertThat(env.acceptsProfiles(Profiles.of("a1 | a2"))).isFalse();

		env.setDefaultProfiles();
		assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
		assertThat(env.acceptsProfiles(Profiles.of("rd1 | rd2"))).isFalse();
		assertThat(env.acceptsProfiles(Profiles.of("d1"))).isFalse();
		assertThat(env.acceptsProfiles(Profiles.of("a1 | a2"))).isFalse();
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
		assertThat(env.getDefaultProfiles()).containsExactly(AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME);
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
				return super.getProperty(key)+"-test";
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

	private Profiles defaultProfile() {
		return Profiles.of(AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME);
	}

}
