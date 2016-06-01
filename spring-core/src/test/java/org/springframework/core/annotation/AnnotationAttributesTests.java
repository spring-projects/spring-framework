/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.AnnotationUtilsTests.ContextConfig;
import org.springframework.core.annotation.AnnotationUtilsTests.ImplicitAliasesContextConfig;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AnnotationAttributes}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1.1
 */
public class AnnotationAttributesTests {

	private AnnotationAttributes attributes = new AnnotationAttributes();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void typeSafeAttributeAccess() {
		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		nestedAttributes.put("value", 10);
		nestedAttributes.put("name", "algernon");

		attributes.put("name", "dave");
		attributes.put("names", new String[] {"dave", "frank", "hal"});
		attributes.put("bool1", true);
		attributes.put("bool2", false);
		attributes.put("color", Color.RED);
		attributes.put("class", Integer.class);
		attributes.put("classes", new Class<?>[] {Number.class, Short.class, Integer.class});
		attributes.put("number", 42);
		attributes.put("anno", nestedAttributes);
		attributes.put("annoArray", new AnnotationAttributes[] {nestedAttributes});

		assertThat(attributes.getString("name"), equalTo("dave"));
		assertThat(attributes.getStringArray("names"), equalTo(new String[] {"dave", "frank", "hal"}));
		assertThat(attributes.getBoolean("bool1"), equalTo(true));
		assertThat(attributes.getBoolean("bool2"), equalTo(false));
		assertThat(attributes.<Color>getEnum("color"), equalTo(Color.RED));
		assertTrue(attributes.getClass("class").equals(Integer.class));
		assertThat(attributes.getClassArray("classes"), equalTo(new Class<?>[] {Number.class, Short.class, Integer.class}));
		assertThat(attributes.<Integer>getNumber("number"), equalTo(42));
		assertThat(attributes.getAnnotation("anno").<Integer>getNumber("value"), equalTo(10));
		assertThat(attributes.getAnnotationArray("annoArray")[0].getString("name"), equalTo("algernon"));

	}

	@Test
	public void unresolvableClass() throws Exception {
		attributes.put("unresolvableClass", new ClassNotFoundException("myclass"));
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(containsString("myclass"));
		attributes.getClass("unresolvableClass");
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
		assertThat(attributes.getStringArray("names"), equalTo(new String[] {"Dogbert"}));
		assertThat(attributes.getClassArray("classes"), equalTo(new Class<?>[] {Number.class}));

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
		attributes.put("filters", new Filter[] {filter, filter});

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
		final String value = "metaverse";

		attributes.clear();
		attributes.put("name", value);
		assertEquals(value, getAliasedString("name"));
		assertEquals(value, getAliasedString("value"));

		attributes.clear();
		attributes.put("value", value);
		assertEquals(value, getAliasedString("name"));
		assertEquals(value, getAliasedString("value"));

		attributes.clear();
		attributes.put("name", value);
		attributes.put("value", value);
		assertEquals(value, getAliasedString("name"));
		assertEquals(value, getAliasedString("value"));
	}

	@Test
	public void getAliasedStringWithImplicitAliases() {
		final String value = "metaverse";
		final List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");

		attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);
		attributes.put("value", value);
		aliases.stream().forEach(alias -> assertEquals(value, getAliasedStringWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", value);
		aliases.stream().forEach(alias -> assertEquals(value, getAliasedStringWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("value", value);
		attributes.put("location1", value);
		attributes.put("xmlFile", value);
		attributes.put("groovyScript", value);
		aliases.stream().forEach(alias -> assertEquals(value, getAliasedStringWithImplicitAliases(alias)));
	}

	@Test
	public void getAliasedStringWithImplicitAliasesWithMissingAliasedAttributes() {
		final List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");
		attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(startsWith("Neither attribute 'value' nor one of its aliases ["));
		aliases.stream().forEach(alias -> exception.expectMessage(containsString(alias)));
		exception.expectMessage(endsWith("] was found in attributes for annotation [" + ImplicitAliasesContextConfig.class.getName() + "]"));
		getAliasedStringWithImplicitAliases("value");
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
		exception.expectMessage(equalTo("Neither attribute 'name' nor one of its aliases [value] was found in attributes for annotation [unknown]"));
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

	private String getAliasedStringWithImplicitAliases(String attributeName) {
		return this.attributes.getAliasedString(attributeName, ImplicitAliasesContextConfig.class, null);
	}

	@Test
	public void getAliasedStringArray() {
		final String[] INPUT = new String[] {"test.xml"};
		final String[] EMPTY = new String[0];

		attributes.clear();
		attributes.put("location", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("location"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("location"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("location", INPUT);
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("location"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("location", INPUT);
		attributes.put("value", EMPTY);
		assertArrayEquals(INPUT, getAliasedStringArray("location"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("location", EMPTY);
		attributes.put("value", INPUT);
		assertArrayEquals(INPUT, getAliasedStringArray("location"));
		assertArrayEquals(INPUT, getAliasedStringArray("value"));

		attributes.clear();
		attributes.put("location", EMPTY);
		attributes.put("value", EMPTY);
		assertArrayEquals(EMPTY, getAliasedStringArray("location"));
		assertArrayEquals(EMPTY, getAliasedStringArray("value"));
	}

	@Test
	public void getAliasedStringArrayWithImplicitAliases() {
		final String[] INPUT = new String[] {"test.xml"};
		final String[] EMPTY = new String[0];
		final List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");

		attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);

		attributes.put("location1", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedStringArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("value", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedStringArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", INPUT);
		attributes.put("value", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedStringArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", INPUT);
		attributes.put("value", EMPTY);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedStringArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", EMPTY);
		attributes.put("value", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedStringArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", EMPTY);
		attributes.put("value", EMPTY);
		aliases.stream().forEach(alias -> assertArrayEquals(EMPTY, getAliasedStringArrayWithImplicitAliases(alias)));
	}

	@Test
	public void getAliasedStringArrayWithImplicitAliasesWithMissingAliasedAttributes() {
		final List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");
		attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(startsWith("Neither attribute 'value' nor one of its aliases ["));
		aliases.stream().forEach(alias -> exception.expectMessage(containsString(alias)));
		exception.expectMessage(endsWith("] was found in attributes for annotation [" + ImplicitAliasesContextConfig.class.getName() + "]"));
		getAliasedStringArrayWithImplicitAliases("value");
	}

	@Test
	public void getAliasedStringArrayWithMissingAliasedAttributes() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(equalTo("Neither attribute 'location' nor one of its aliases [value] was found in attributes for annotation [unknown]"));
		getAliasedStringArray("location");
	}

	@Test
	public void getAliasedStringArrayWithDifferentAliasedValues() {
		attributes.put("location", new String[] {"1.xml"});
		attributes.put("value", new String[] {"2.xml"});

		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(containsString("In annotation [" + ContextConfig.class.getName() + "]"));
		exception.expectMessage(containsString("attribute [location] and its alias [value]"));
		exception.expectMessage(containsString("[{1.xml}] and [{2.xml}]"));
		exception.expectMessage(containsString("but only one is permitted"));

		getAliasedStringArray("location");
	}

	private String[] getAliasedStringArray(String attributeName) {
		// Note: even though the attributes we test against here are of type
		// String instead of String[], it doesn't matter... since
		// AnnotationAttributes does not validate the actual return type of
		// attributes in the annotation.
		return attributes.getAliasedStringArray(attributeName, ContextConfig.class, null);
	}

	private String[] getAliasedStringArrayWithImplicitAliases(String attributeName) {
		// Note: even though the attributes we test against here are of type
		// String instead of String[], it doesn't matter... since
		// AnnotationAttributes does not validate the actual return type of
		// attributes in the annotation.
		return this.attributes.getAliasedStringArray(attributeName, ImplicitAliasesContextConfig.class, null);
	}

	@Test
	public void getAliasedClassArray() {
		final Class<?>[] INPUT = new Class<?>[] {String.class};
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
	public void getAliasedClassArrayWithImplicitAliases() {
		final Class<?>[] INPUT = new Class<?>[] {String.class};
		final Class<?>[] EMPTY = new Class<?>[0];
		final List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");

		attributes = new AnnotationAttributes(ImplicitAliasesContextConfig.class);

		attributes.put("location1", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedClassArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("value", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedClassArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", INPUT);
		attributes.put("value", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedClassArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", INPUT);
		attributes.put("value", EMPTY);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedClassArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", EMPTY);
		attributes.put("value", INPUT);
		aliases.stream().forEach(alias -> assertArrayEquals(INPUT, getAliasedClassArrayWithImplicitAliases(alias)));

		attributes.clear();
		attributes.put("location1", EMPTY);
		attributes.put("value", EMPTY);
		aliases.stream().forEach(alias -> assertArrayEquals(EMPTY, getAliasedClassArrayWithImplicitAliases(alias)));
	}

	@Test
	public void getAliasedClassArrayWithMissingAliasedAttributes() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(equalTo("Neither attribute 'classes' nor one of its aliases [value] was found in attributes for annotation [unknown]"));
		getAliasedClassArray("classes");
	}

	@Test
	public void getAliasedClassArrayWithDifferentAliasedValues() {
		attributes.put("classes", new Class<?>[] {String.class});
		attributes.put("value", new Class<?>[] {Number.class});

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

	private Class<?>[] getAliasedClassArrayWithImplicitAliases(String attributeName) {
		// Note: even though the attributes we test against here are of type
		// String instead of Class<?>[], it doesn't matter... since
		// AnnotationAttributes does not validate the actual return type of
		// attributes in the annotation.
		return this.attributes.getAliasedClassArray(attributeName, ImplicitAliasesContextConfig.class, null);
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
