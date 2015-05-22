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

	enum Color {
		RED, WHITE, BLUE
	}

	private final AnnotationAttributes attributes = new AnnotationAttributes();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void typeSafeAttributeAccess() {
		attributes.put("name", "dave");
		attributes.put("names", new String[] { "dave", "frank", "hal" });
		attributes.put("bool1", true);
		attributes.put("bool2", false);
		attributes.put("color", Color.RED);
		attributes.put("clazz", Integer.class);
		attributes.put("classes", new Class<?>[] { Number.class, Short.class, Integer.class });
		attributes.put("number", 42);
		attributes.put("numbers", new int[] { 42, 43 });
		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		nestedAttributes.put("value", 10);
		nestedAttributes.put("name", "algernon");
		attributes.put("anno", nestedAttributes);
		attributes.put("annoArray", new AnnotationAttributes[] { nestedAttributes });

		assertThat(attributes.getString("name"), equalTo("dave"));
		assertThat(attributes.getStringArray("names"), equalTo(new String[] { "dave", "frank", "hal" }));
		assertThat(attributes.getBoolean("bool1"), equalTo(true));
		assertThat(attributes.getBoolean("bool2"), equalTo(false));
		assertThat(attributes.<Color>getEnum("color"), equalTo(Color.RED));
		assertTrue(attributes.getClass("clazz").equals(Integer.class));
		assertThat(attributes.getClassArray("classes"), equalTo(new Class[] { Number.class, Short.class, Integer.class }));
		assertThat(attributes.<Integer>getNumber("number"), equalTo(42));
		assertThat(attributes.getAnnotation("anno").<Integer>getNumber("value"), equalTo(10));
		assertThat(attributes.getAnnotationArray("annoArray")[0].getString("name"), equalTo("algernon"));
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

}
