/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AnnotationAttributes}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1.1
 */
public class AnnotationAttributesTests {

	private final AnnotationAttributes attributes = new AnnotationAttributes();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void typeSafeAttributeAccess() {
		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		nestedAttributes.put("value", 10);
		nestedAttributes.put("name", "algernon");

		attributes.put("name", "dave");
		attributes.put("names", new String[] { "dave", "frank", "hal" });
		attributes.put("bool1", true);
		attributes.put("bool2", false);
		attributes.put("color", Color.RED);
		attributes.put("class", Integer.class);
		attributes.put("classes", new Class<?>[] { Number.class, Short.class, Integer.class });
		attributes.put("number", 42);
		attributes.put("anno", nestedAttributes);
		attributes.put("annoArray", new AnnotationAttributes[] { nestedAttributes });
		attributes.put("unresolvableClass", new ClassNotFoundException("myclass"));

		assertThat(attributes.getString("name"), equalTo("dave"));
		assertThat(attributes.getStringArray("names"), equalTo(new String[] { "dave", "frank", "hal" }));
		assertThat(attributes.getBoolean("bool1"), equalTo(true));
		assertThat(attributes.getBoolean("bool2"), equalTo(false));
		assertThat(attributes.<Color>getEnum("color"), equalTo(Color.RED));
		assertTrue(attributes.getClass("class").equals(Integer.class));
		assertThat(attributes.getClassArray("classes"), equalTo(new Class[] { Number.class, Short.class, Integer.class }));
		assertThat(attributes.<Integer>getNumber("number"), equalTo(42));
		assertThat(attributes.getAnnotation("anno").<Integer>getNumber("value"), equalTo(10));
		assertThat(attributes.getAnnotationArray("annoArray")[0].getString("name"), equalTo("algernon"));

		try {
			attributes.getClass("unresolvableClass");
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getCause() instanceof ClassNotFoundException);
			assertTrue(ex.getMessage().contains("myclass"));
		}
	}

	@Test
	public void singleElementToSingleElementArrayConversionSupport() throws Exception {
		Filter filter = FilteredClass.class.getAnnotation(Filter.class);

		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		nestedAttributes.put("name", "Dilbert");

		// Store single elements
		attributes.put("names", "Dogbert");
		attributes.put("classes", Number.class);
		attributes.put("nestedAttributes", nestedAttributes);
		attributes.put("filters", filter);

		// Get back arrays of single elements
		assertThat(attributes.getStringArray("names"), equalTo(new String[] { "Dogbert" }));
		assertThat(attributes.getClassArray("classes"), equalTo(new Class[] { Number.class }));

		AnnotationAttributes[] array = attributes.getAnnotationArray("nestedAttributes");
		assertNotNull(array);
		assertThat(array.length, is(1));
		assertThat(array[0].getString("name"), equalTo("Dilbert"));

		Filter[] filters = attributes.getAnnotationArray("filters", Filter.class);
		assertNotNull(filters);
		assertThat(filters.length, is(1));
		assertThat(filters[0].pattern(), equalTo("foo"));
	}

	@Test
	public void nestedAnnotations() throws Exception {
		Filter filter = FilteredClass.class.getAnnotation(Filter.class);

		attributes.put("filter", filter);
		attributes.put("filters", new Filter[] { filter, filter });

		Filter retrievedFilter = attributes.getAnnotation("filter", Filter.class);
		assertThat(retrievedFilter, equalTo(filter));
		assertThat(retrievedFilter.pattern(), equalTo("foo"));

		Filter[] retrievedFilters = attributes.getAnnotationArray("filters", Filter.class);
		assertNotNull(retrievedFilters);
		assertEquals(2, retrievedFilters.length);
		assertThat(retrievedFilters[1].pattern(), equalTo("foo"));
	}

	@Test
	public void getEnumWithNullAttributeName() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(containsString("attributeName must not be null or empty"));
		attributes.getEnum(null);
	}

	@Test
	public void getEnumWithEmptyAttributeName() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(containsString("attributeName must not be null or empty"));
		attributes.getEnum("");
	}

	@Test
	public void getEnumWithUnknownAttributeName() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(containsString("Attribute 'bogus' not found"));
		attributes.getEnum("bogus");
	}

	@Test
	public void getEnumWithTypeMismatch() {
		attributes.put("color", "RED");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(containsString("Attribute 'color' is of type [String], but [Enum] was expected"));
		attributes.getEnum("color");
	}

	@Test
	public void getAliasedString() {
		attributes.clear();
		attributes.put("name", "metaverse");
		assertEquals("metaverse", getAliasedString("name"));
		assertEquals("metaverse", getAliasedString("value"));

		attributes.clear();
		attributes.put("value", "metaverse");
		assertEquals("metaverse", getAliasedString("name"));
		assertEquals("metaverse", getAliasedString("value"));

		attributes.clear();
		attributes.put("name", "metaverse");
		attributes.put("value", "metaverse");
		assertEquals("metaverse", getAliasedString("name"));
		assertEquals("metaverse", getAliasedString("value"));
	}

	@Test
	public void getAliasedStringFromSynthesizedAnnotationAttributes() {
		Scope scope = ScopedComponent.class.getAnnotation(Scope.class);
		AnnotationAttributes scopeAttributes = AnnotationUtils.getAnnotationAttributes(ScopedComponent.class, scope);

		assertEquals("custom", getAliasedString(scopeAttributes, "name"));
		assertEquals("custom", getAliasedString(scopeAttributes, "value"));
	}

	@Test
	public void getAliasedStringWithMissingAliasedAttributes() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(equalTo("Neither attribute 'name' nor its alias 'value' was found in attributes for annotation [unknown]"));
		getAliasedString("name");
	}

	@Test
	public void getAliasedStringWithDifferentAliasedValues() {
		attributes.put("name", "request");
		attributes.put("value", "session");

		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(containsString("In annotation [" + Scope.class.getName() + "]"));
		exception.expectMessage(containsString("attribute [name] and its alias [value]"));
		exception.expectMessage(containsString("[request] and [session]"));
		exception.expectMessage(containsString("but only one is permitted"));

		getAliasedString("name");
	}

	private String getAliasedString(String attributeName) {
		return getAliasedString(this.attributes, attributeName);
	}

	private String getAliasedString(AnnotationAttributes attrs, String attributeName) {
		return attrs.getAliasedString(attributeName, Scope.class, null);
	}

	@Test
	public void getAliasedStringArray() {
		final String[] INPUT = new String[] { "test.xml" };
		final String[] EMPTY = new String[0];

		attributes.clear();
		attributes.put("locations", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("locations"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("locations"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("locations", INPUT);
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("locations"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("locations", INPUT);
		attributes.put("value", EMPTY);
		assertArrayEquals(INPUT, getAliasedStringArray("locations"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("locations", EMPTY);
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("locations"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("locations", EMPTY);
		attributes.put("value", EMPTY);
		assertArrayEquals(EMPTY, getAliasedStringArray("locations"));
		assertArrayEquals(EMPTY, getAliasedStringArray("value"));
	}

	@Test
	public void getAliasedStringArrayWithMissingAliasedAttributes() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(equalTo("Neither attribute 'locations' nor its alias 'value' was found in attributes for annotation [unknown]"));
		getAliasedStringArray("locations");
	}

	@Test
	public void getAliasedStringArrayWithDifferentAliasedValues() {
		attributes.put("locations", new String[] { "1.xml" });
		attributes.put("value", new String[] { "2.xml" });

		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(containsString("In annotation [" + ContextConfig.class.getName() + "]"));
		exception.expectMessage(containsString("attribute [locations] and its alias [value]"));
		exception.expectMessage(containsString("[{1.xml}] and [{2.xml}]"));
		exception.expectMessage(containsString("but only one is permitted"));

		getAliasedStringArray("locations");
	}

	private String[] getAliasedStringArray(String attributeName) {
		return attributes.getAliasedStringArray(attributeName, ContextConfig.class, null);
	}

	@Test
	public void getAliasedClassArray() {
		final Class<?>[] INPUT = new Class<?>[] { String.class };
		final Class<?>[] EMPTY = new Class<?>[0];

		attributes.clear();
		attributes.put("classes", INPUT);
		assertArrayEquals(INPUT, getAliasedClassArray("classes"));
		assertArrayEquals(INPUT, getAliasedClassArray("value"));

		attributes.clear();
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedClassArray("classes"));
		assertArrayEquals(INPUT, getAliasedClassArray("value"));

		attributes.clear();
		attributes.put("classes", INPUT);
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedClassArray("classes"));
		assertArrayEquals(INPUT, getAliasedClassArray("value"));

		attributes.clear();
		attributes.put("classes", INPUT);
		attributes.put("value", EMPTY);
		assertArrayEquals(INPUT, getAliasedClassArray("classes"));
		assertArrayEquals(INPUT, getAliasedClassArray("value"));

		attributes.clear();
		attributes.put("classes", EMPTY);
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedClassArray("classes"));
		assertArrayEquals(INPUT, getAliasedClassArray("value"));

		attributes.clear();
		attributes.put("classes", EMPTY);
		attributes.put("value", EMPTY);
		assertArrayEquals(EMPTY, getAliasedClassArray("classes"));
		assertArrayEquals(EMPTY, getAliasedClassArray("value"));
	}

	@Test
	public void getAliasedClassArrayWithMissingAliasedAttributes() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(equalTo("Neither attribute 'classes' nor its alias 'value' was found in attributes for annotation [unknown]"));
		getAliasedClassArray("classes");
	}

	@Test
	public void getAliasedClassArrayWithDifferentAliasedValues() {
		attributes.put("classes", new Class[] { String.class });
		attributes.put("value", new Class[] { Number.class });

		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(containsString("In annotation [" + Filter.class.getName() + "]"));
		exception.expectMessage(containsString("attribute [classes] and its alias [value]"));
		exception.expectMessage(containsString("[{class java.lang.String}] and [{class java.lang.Number}]"));
		exception.expectMessage(containsString("but only one is permitted"));

		getAliasedClassArray("classes");
	}

	private Class<?>[] getAliasedClassArray(String attributeName) {
		return attributes.getAliasedClassArray(attributeName, Filter.class, null);
	}


	enum Color {
		RED, WHITE, BLUE
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Filter {

		@AliasFor(attribute = "classes")
		Class<?>[] value() default {};

		@AliasFor(attribute = "value")
		Class<?>[] classes() default {};

		String pattern();
	}

	@Filter(pattern = "foo")
	static class FilteredClass {
	}

	/**
	 * Mock of {@code org.springframework.test.context.ContextConfiguration}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextConfig {

		@AliasFor(attribute = "locations")
		String value() default "";

		@AliasFor(attribute = "value")
		String locations() default "";
	}

	/**
	 * Mock of {@code org.springframework.context.annotation.Scope}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface Scope {

		@AliasFor(attribute = "name")
		String value() default "singleton";

		@AliasFor(attribute = "value")
		String name() default "singleton";
	}

	@Scope(name = "custom")
	static class ScopedComponent {
	}

}
