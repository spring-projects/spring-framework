/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.env;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.convert.ConversionException;
import org.springframework.mock.env.MockPropertySource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link PropertySourcesPropertyResolver}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class PropertySourcesPropertyResolverTests {
	private Properties testProperties;
	private MutablePropertySources propertySources;
	private ConfigurablePropertyResolver propertyResolver;

	@Before
	public void setUp() {
		propertySources = new MutablePropertySources();
		propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		testProperties = new Properties();
		propertySources.addFirst(new PropertiesPropertySource("testProperties", testProperties));
	}

	@Test
	public void containsProperty() {
		assertThat(propertyResolver.containsProperty("foo"), is(false));
		testProperties.put("foo", "bar");
		assertThat(propertyResolver.containsProperty("foo"), is(true));
	}

	@Test
	public void getProperty() {
		assertThat(propertyResolver.getProperty("foo"), nullValue());
		testProperties.put("foo", "bar");
		assertThat(propertyResolver.getProperty("foo"), is("bar"));
	}

	@Test
	public void getProperty_withDefaultValue() {
		assertThat(propertyResolver.getProperty("foo", "myDefault"), is("myDefault"));
		testProperties.put("foo", "bar");
		assertThat(propertyResolver.getProperty("foo"), is("bar"));
	}

	@Test
	public void getProperty_propertySourceSearchOrderIsFIFO() {
		MutablePropertySources sources = new MutablePropertySources();
		PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
		sources.addFirst(new MockPropertySource("ps1").withProperty("pName", "ps1Value"));
		assertThat(resolver.getProperty("pName"), equalTo("ps1Value"));
		sources.addFirst(new MockPropertySource("ps2").withProperty("pName", "ps2Value"));
		assertThat(resolver.getProperty("pName"), equalTo("ps2Value"));
		sources.addFirst(new MockPropertySource("ps3").withProperty("pName", "ps3Value"));
		assertThat(resolver.getProperty("pName"), equalTo("ps3Value"));
	}

	@Test
	public void getProperty_withExplicitNullValue() {
		// java.util.Properties does not allow null values (because Hashtable does not)
		Map<String, Object> nullableProperties = new HashMap<String, Object>();
		propertySources.addLast(new MapPropertySource("nullableProperties", nullableProperties));
		nullableProperties.put("foo", null);
		assertThat(propertyResolver.getProperty("foo"), nullValue());
	}

	@Test
	public void getProperty_withTargetType_andDefaultValue() {
		assertThat(propertyResolver.getProperty("foo", Integer.class, 42), equalTo(42));
		testProperties.put("foo", 13);
		assertThat(propertyResolver.getProperty("foo", Integer.class, 42), equalTo(13));
	}

	@Test
	public void getProperty_withStringArrayConversion() {
		testProperties.put("foo", "bar,baz");
		assertThat(propertyResolver.getProperty("foo", String[].class), equalTo(new String[] { "bar", "baz" }));
	}


	@Test
	public void getProperty_withNonConvertibleTargetType() {
		testProperties.put("foo", "bar");

		class TestType { }

		try {
			propertyResolver.getProperty("foo", TestType.class);
			fail("Expected IllegalArgumentException due to non-convertible types");
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void getProperty_doesNotCache_replaceExistingKeyPostConstruction() {
		String key = "foo";
		String value1 = "bar";
		String value2 = "biz";

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(key, value1); // before construction
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource("testProperties", map));
		PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(propertyResolver.getProperty(key), equalTo(value1));
		map.put(key, value2); // after construction and first resolution
		assertThat(propertyResolver.getProperty(key), equalTo(value2));
	}

	@Test
	public void getProperty_doesNotCache_addNewKeyPostConstruction() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource("testProperties", map));
		PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(propertyResolver.getProperty("foo"), equalTo(null));
		map.put("foo", "42");
		assertThat(propertyResolver.getProperty("foo"), equalTo("42"));
	}

	@Test
	public void getPropertySources_replacePropertySource() {
		propertySources = new MutablePropertySources();
		propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		propertySources.addLast(new MockPropertySource("local").withProperty("foo", "localValue"));
		propertySources.addLast(new MockPropertySource("system").withProperty("foo", "systemValue"));

		// 'local' was added first so has precedence
		assertThat(propertyResolver.getProperty("foo"), equalTo("localValue"));

		// replace 'local' with new property source
		propertySources.replace("local", new MockPropertySource("new").withProperty("foo", "newValue"));

		// 'system' now has precedence
		assertThat(propertyResolver.getProperty("foo"), equalTo("newValue"));

		assertThat(propertySources.size(), is(2));
	}

	@Test
	public void getRequiredProperty() {
		testProperties.put("exists", "xyz");
		assertThat(propertyResolver.getRequiredProperty("exists"), is("xyz"));

		try {
			propertyResolver.getRequiredProperty("bogus");
			fail("expected IllegalStateException");
		} catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void getRequiredProperty_withStringArrayConversion() {
		testProperties.put("exists", "abc,123");
		assertThat(propertyResolver.getRequiredProperty("exists", String[].class), equalTo(new String[] { "abc", "123" }));

		try {
			propertyResolver.getRequiredProperty("bogus", String[].class);
			fail("expected IllegalStateException");
		} catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void resolvePlaceholders() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key}"), equalTo("Replace this value"));
	}

	@Test
	public void resolvePlaceholders_withUnresolvable() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown}"),
				equalTo("Replace this value plus ${unknown}"));
	}

	@Test
	public void resolvePlaceholders_withDefaultValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown:defaultValue}"),
				equalTo("Replace this value plus defaultValue"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void resolvePlaceholders_withNullInput() {
		new PropertySourcesPropertyResolver(new MutablePropertySources()).resolvePlaceholders(null);
	}

	@Test
	public void resolveRequiredPlaceholders() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key}"), equalTo("Replace this value"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void resolveRequiredPlaceholders_withUnresolvable() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown}");
	}

	@Test
	public void resolveRequiredPlaceholders_withDefaultValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown:defaultValue}"),
				equalTo("Replace this value plus defaultValue"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void resolveRequiredPlaceholders_withNullInput() {
		new PropertySourcesPropertyResolver(new MutablePropertySources()).resolveRequiredPlaceholders(null);
	}

	@Test
	public void getPropertyAsClass() throws ClassNotFoundException, LinkageError {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", SpecificType.class.getName()));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertTrue(resolver.getPropertyAsClass("some.class", SomeType.class).equals(SpecificType.class));
	}

	@Test
	public void getPropertyAsClass_withInterfaceAsTarget() throws ClassNotFoundException, LinkageError {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", SomeType.class.getName()));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertTrue(resolver.getPropertyAsClass("some.class", SomeType.class).equals(SomeType.class));
	}

	@Test(expected=ConversionException.class)
	public void getPropertyAsClass_withMismatchedTypeForValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", "java.lang.String"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		resolver.getPropertyAsClass("some.class", SomeType.class);
	}

	@Test(expected=ConversionException.class)
	public void getPropertyAsClass_withNonExistentClassForValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", "some.bogus.Class"));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		resolver.getPropertyAsClass("some.class", SomeType.class);
	}

	@Test
	public void getPropertyAsClass_withObjectForValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", new SpecificType()));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertTrue(resolver.getPropertyAsClass("some.class", SomeType.class).equals(SpecificType.class));
	}

	@Test(expected=ConversionException.class)
	public void getPropertyAsClass_withMismatchedObjectForValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", new Integer(42)));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		resolver.getPropertyAsClass("some.class", SomeType.class);
	}

	@Test
	public void getPropertyAsClass_withRealClassForValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", SpecificType.class));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		assertTrue(resolver.getPropertyAsClass("some.class", SomeType.class).equals(SpecificType.class));
	}

	@Test(expected=ConversionException.class)
	public void getPropertyAsClass_withMismatchedRealClassForValue() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MockPropertySource().withProperty("some.class", Integer.class));
		PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
		resolver.getPropertyAsClass("some.class", SomeType.class);
	}

	@Test
	public void setRequiredProperties_andValidateRequiredProperties() {
		// no properties have been marked as required -> validation should pass
		propertyResolver.validateRequiredProperties();

		// mark which properties are required
		propertyResolver.setRequiredProperties("foo", "bar");

		// neither foo nor bar properties are present -> validating should throw
		try {
			propertyResolver.validateRequiredProperties();
			fail("expected validation exception");
		} catch (MissingRequiredPropertiesException ex) {
			assertThat(ex.getMessage(), equalTo(
					"The following properties were declared as required " +
					"but could not be resolved: [foo, bar]"));
		}

		// add foo property -> validation should fail only on missing 'bar' property
		testProperties.put("foo", "fooValue");
		try {
			propertyResolver.validateRequiredProperties();
			fail("expected validation exception");
		} catch (MissingRequiredPropertiesException ex) {
			assertThat(ex.getMessage(), equalTo(
					"The following properties were declared as required " +
					"but could not be resolved: [bar]"));
		}

		// add bar property -> validation should pass, even with an empty string value
		testProperties.put("bar", "");
		propertyResolver.validateRequiredProperties();
	}

	@Test
	public void resolveNestedPropertyPlaceholders() {
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
		PropertySourcesPropertyResolver pr = new PropertySourcesPropertyResolver(ps);
		assertThat(pr.getProperty("p1"), equalTo("v1"));
		assertThat(pr.getProperty("p2"), equalTo("v2"));
		assertThat(pr.getProperty("p3"), equalTo("v1:v2"));
		assertThat(pr.getProperty("p4"), equalTo("v1:v2"));
		try {
			pr.getProperty("p5");
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), Matchers.containsString(
					"Could not resolve placeholder 'bogus' in string value [${p1}:${p2}:${bogus}]"));
		}
		assertThat(pr.getProperty("p6"), equalTo("v1:v2:def"));
		try {
			pr.getProperty("pL");
		} catch (StackOverflowError ex) {
			// no explicit handling for cyclic references for now
		}
	}


	static interface SomeType { }
	static class SpecificType implements SomeType { }
}
