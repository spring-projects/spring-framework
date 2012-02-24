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

package org.springframework.core.annotation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for {@link AnnotationAttributes}.
 *
 * @author Chris Beams
 * @since 3.1.1
 */
public class AnnotationAttributesTests {

	enum Color { RED, WHITE, BLUE }

	@Test
	public void testTypeSafeAttributeAccess() {
		AnnotationAttributes a = new AnnotationAttributes();
		a.put("name", "dave");
		a.put("names", new String[] { "dave", "frank", "hal" });
		a.put("bool1", true);
		a.put("bool2", false);
		a.put("color", Color.RED);
		a.put("clazz", Integer.class);
		a.put("classes", new Class<?>[] { Number.class, Short.class, Integer.class });
		a.put("number", 42);
		a.put("numbers", new int[] { 42, 43 });
		AnnotationAttributes anno = new AnnotationAttributes();
		anno.put("value", 10);
		anno.put("name", "algernon");
		a.put("anno", anno);
		a.put("annoArray", new AnnotationAttributes[] { anno });

		assertThat(a.getString("name"), equalTo("dave"));
		assertThat(a.getStringArray("names"), equalTo(new String[] { "dave", "frank", "hal" }));
		assertThat(a.getBoolean("bool1"), equalTo(true));
		assertThat(a.getBoolean("bool2"), equalTo(false));
		assertThat(a.<Color>getEnum("color"), equalTo(Color.RED));
		assertTrue(a.getClass("clazz").equals(Integer.class));
		assertThat(a.getClassArray("classes"), equalTo(new Class[] { Number.class, Short.class, Integer.class }));
		assertThat(a.<Integer>getNumber("number"), equalTo(42));
		assertThat(a.getAnnotation("anno").<Integer>getNumber("value"), equalTo(10));
		assertThat(a.getAnnotationArray("annoArray")[0].getString("name"), equalTo("algernon"));
	}

	@Test
	public void getEnum_emptyAttributeName() {
		AnnotationAttributes a = new AnnotationAttributes();
		a.put("color", "RED");
		try {
			a.getEnum("");
			fail();
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), equalTo("attributeName must not be null or empty"));
		}
		try {
			a.getEnum(null);
			fail();
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), equalTo("attributeName must not be null or empty"));
		}
	}

	@Test
	public void getEnum_notFound() {
		AnnotationAttributes a = new AnnotationAttributes();
		a.put("color", "RED");
		try {
			a.getEnum("colour");
			fail();
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), equalTo("Attribute 'colour' not found"));
		}
	}

	@Test
	public void getEnum_typeMismatch() {
		AnnotationAttributes a = new AnnotationAttributes();
		a.put("color", "RED");
		try {
			a.getEnum("color");
			fail();
		} catch (IllegalArgumentException ex) {
			String expected =
					"Attribute 'color' is of type [String], but [Enum] was expected";
			assertThat(ex.getMessage().substring(0, expected.length()), equalTo(expected));
		}
	}

}
