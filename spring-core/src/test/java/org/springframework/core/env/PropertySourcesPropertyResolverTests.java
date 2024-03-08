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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.testfixture.env.MockPropertySource;
import org.springframework.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Chris Beams
 * @since 3.1
 */
class PropertySourcesPropertyResolverTests {

	private Properties testProperties;

	private MutablePropertySources propertySources;

	private ConfigurablePropertyResolver propertyResolver;


	@BeforeEach
	void setUp() {
		propertySources = new MutablePropertySources();
		propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		testProperties = new Properties();
		propertySources.addFirst(new PropertiesPropertySource("testProperties", testProperties));
	}


	@Test
	void containsProperty() {
		assertThat(propertyResolver.containsProperty("foo")).isFalse();
		testProperties.put("foo", "bar");
		assertThat(propertyResolver.containsProperty("foo")).isTrue();
	}

	@Test
	void getProperty() {
		assertThat(propertyResolver.getProperty("foo")).isNull();
		testProperties.put("foo", "bar");
		assertThat(propertyResolver.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void getProperty_withDefaultValue() {
		assertThat(propertyResolver.getProperty("foo", "myDefault")).isEqualTo("myDefault");
		testProperties.put("foo", "bar");
		assertThat(propertyResolver.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void getProperty_propertySourceSearchOrderIsFIFO() {
		MutablePropertySources sources = new MutablePropertySources();
		PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
		sources.addFirst(new MockPropertySource("ps1").withProperty("pName", "ps1Value"));
		assertThat(resolver.getProperty("pName")).isEqualTo("ps1Value");
		sources.addFirst(new MockPropertySource("ps2").withProperty("pName", "ps2Value"));
		assertThat(resolver.getProperty("pName")).isEqualTo("ps2Value");
		sources.addFirst(new MockPropertySource("ps3").withProperty("pName", "ps3Value"));
		assertThat(resolver.getProperty("pName")).isEqualTo("ps3Value");
	}

	@Test
	void getProperty_withExplicitNullValue() {
		// java.util.Properties does not allow null values (because Hashtable does not)
		Map<String, Object> nullableProperties = new HashMap<>();
		propertySources.addLast(new MapPropertySource("nullableProperties", nullableProperties));
		nullableProperties.put("foo", null);
		assertThat(propertyResolver.getProperty("foo")).isNull();
	}

	@Test
	void getProperty_withTargetType_andDefaultValue() {
		assertThat(propertyResolver.getProperty("foo", Integer.class, 42)).isEqualTo(42);
		testProperties.put("foo", 13);
		assertThat(propertyResolver.getProperty("foo", Integer.class, 42)).isEqualTo(13);
	}

	@Test
	void getProperty_withStringArrayConversion() {
		testProperties.put("foo", "bar,baz");
		assertThat(propertyResolver.getProperty("foo", String[].class)).isEqualTo(new String[] { "bar", "baz" });
	}

	@Test
	void getProperty_withNonConvertibleTargetType() {
		testProperties.put("foo", "bar");

		class TestType { }

		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				propertyResolver.getProperty("foo", TestType.class));
	}

	@Test
	void getProperty_doesNotCache_replaceExistingKeyPostConstruction() {
		String key = "foo";
		String value1 = "bar";
		String value2 = "biz";

		HashMap<String, Object> map = new HashMap<>();
		map.put(key, value1); // before construction
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource("testProperties", map));
		PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(propertyResolver.getProperty(key)).isEqualTo(value1);
		map.put(key, value2); // after construction and first resolution
		assertThat(propertyResolver.getProperty(key)).isEqualTo(value2);
	}

	@Test
	void getProperty_doesNotCache_addNewKeyPostConstruction() {
		HashMap<String, Object> map = new HashMap<>();
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource("testProperties", map));
		PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(propertyResolver.getProperty("foo")).isNull();
		map.put("foo", "42");
		assertThat(propertyResolver.getProperty("foo")).isEqualTo("42");
	}

	@Test
	void getPropertySources_replacePropertySource() {
		propertySources = new MutablePropertySources();
		propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		propertySources.addLast(new MockPropertySource("local").withProperty("foo", "localValue"));
		propertySources.addLast(new MockPropertySource("system").withProperty("foo", "systemValue"));

		// 'local' was added first so has precedence
		assertThat(propertyResolver.getProperty("foo")).isEqualTo("localValue");

		// replace 'local' with new property source
		propertySources.replace("local", new MockPropertySource("new").withProperty("foo", "newValue"));

		// 'system' now has precedence
		assertThat(propertyResolver.getProperty("foo")).isEqualTo("newValue");

		assertThat(propertySources).hasSize(2);
	}

	@Test
	void getRequiredProperty() {
		testProperties.put("exists", "xyz");
		assertThat(propertyResolver.getRequiredProperty("exists")).isEqualTo("xyz");

		assertThatIllegalStateException().isThrownBy(() ->
				propertyResolver.getRequiredProperty("bogus"));
	}

	@Test
	void getRequiredProperty_withStringArrayConversion() {
		testProperties.put("exists", "abc,123");
		assertThat(propertyResolver.getRequiredProperty("exists", String[].class)).isEqualTo(new String[] { "abc", "123" });

		assertThatIllegalStateException().isThrownBy(() ->
				propertyResolver.getRequiredProperty("bogus", String[].class));
	}

	@Test
	void resolvePlaceholders() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key}")).isEqualTo("Replace this value");
	}

	@Test
	void resolvePlaceholders_withUnresolvable() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown}"))
				.isEqualTo("Replace this value plus ${unknown}");
	}

	@Test
	void resolvePlaceholders_withDefaultValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown:defaultValue}"))
				.isEqualTo("Replace this value plus defaultValue");
	}

	@Test
	void resolvePlaceholders_withNullInput() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertySourcesPropertyResolver(new MutablePropertySources()).resolvePlaceholders(null));
	}

	@Test
	void resolveRequiredPlaceholders() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key}")).isEqualTo("Replace this value");
	}

	@Test
	void resolveRequiredPlaceholders_withUnresolvable() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
				resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown}"));
	}

	@Test
	void resolveRequiredPlaceholders_withDefaultValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown:defaultValue}"))
				.isEqualTo("Replace this value plus defaultValue");
	}

	@Test
	void resolveRequiredPlaceholders_withNullInput() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertySourcesPropertyResolver(new MutablePropertySources()).resolveRequiredPlaceholders(null));
	}

	@Test
	void setRequiredProperties_andValidateRequiredProperties() {
		// no properties have been marked as required -> validation should pass
		propertyResolver.validateRequiredProperties();

		// mark which properties are required
		propertyResolver.setRequiredProperties("foo", "bar");

		// neither foo nor bar properties are present -> validating should throw
		assertThatExceptionOfType(MissingRequiredPropertiesException.class).isThrownBy(
				propertyResolver::validateRequiredProperties)
			.withMessage("The following properties were declared as required " +
					"but could not be resolved: [foo, bar]");

		// add foo property -> validation should fail only on missing 'bar' property
		testProperties.put("foo", "fooValue");
		assertThatExceptionOfType(MissingRequiredPropertiesException.class).isThrownBy(
				propertyResolver::validateRequiredProperties)
			.withMessage("The following properties were declared as required " +
					"but could not be resolved: [bar]");

		// add bar property -> validation should pass, even with an empty string value
		testProperties.put("bar", "");
		propertyResolver.validateRequiredProperties();
	}

	@Test
	void resolveNestedPropertyPlaceholders() {
		MutablePropertySources ps = new MutablePropertySources();
		ps.addFirst(new MockPropertySource()
			.withProperty("p1", "v1")
			.withProperty("p2", "v2")
			.withProperty("p3", "${p1}:${p2}")              // nested placeholders
			.withProperty("p4", "${p3}")                    // deeply nested placeholders
			.withProperty("p5", "${p1}:${p2}:${bogus}")     // unresolvable placeholder
			.withProperty("p6", "${p1}:${p2}:${bogus:def}") // unresolvable w/ default
			.withProperty("pL", "${pR}")                    // cyclic reference left
			.withProperty("pR", "${pL}")                    // cyclic reference right
		);
		ConfigurablePropertyResolver pr = new PropertySourcesPropertyResolver(ps);
		assertThat(pr.getProperty("p1")).isEqualTo("v1");
		assertThat(pr.getProperty("p2")).isEqualTo("v2");
		assertThat(pr.getProperty("p3")).isEqualTo("v1:v2");
		assertThat(pr.getProperty("p4")).isEqualTo("v1:v2");
		assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
				pr.getProperty("p5"))
			.withMessageContaining("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\"");
		assertThat(pr.getProperty("p6")).isEqualTo("v1:v2:def");
		assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
				pr.getProperty("pL"))
			.withMessageContaining("Circular");
	}

	@Test
	void ignoreUnresolvableNestedPlaceholdersIsConfigurable() {
		MutablePropertySources ps = new MutablePropertySources();
		ps.addFirst(new MockPropertySource()
			.withProperty("p1", "v1")
			.withProperty("p2", "v2")
			.withProperty("p3", "${p1}:${p2}:${bogus:def}") // unresolvable w/ default
			.withProperty("p4", "${p1}:${p2}:${bogus}")     // unresolvable placeholder
		);
		ConfigurablePropertyResolver pr = new PropertySourcesPropertyResolver(ps);
		assertThat(pr.getProperty("p1")).isEqualTo("v1");
		assertThat(pr.getProperty("p2")).isEqualTo("v2");
		assertThat(pr.getProperty("p3")).isEqualTo("v1:v2:def");

		// placeholders nested within the value of "p4" are unresolvable and cause an
		// exception by default
		assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
				pr.getProperty("p4"))
			.withMessageContaining("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\"");

		// relax the treatment of unresolvable nested placeholders
		pr.setIgnoreUnresolvableNestedPlaceholders(true);
		// and observe they now pass through unresolved
		assertThat(pr.getProperty("p4")).isEqualTo("v1:v2:${bogus}");

		// resolve[Nested]Placeholders methods behave as usual regardless the value of
		// ignoreUnresolvableNestedPlaceholders
		assertThat(pr.resolvePlaceholders("${p1}:${p2}:${bogus}")).isEqualTo("v1:v2:${bogus}");
		assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
				pr.resolveRequiredPlaceholders("${p1}:${p2}:${bogus}"))
			.withMessageContaining("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\"");
	}

}
