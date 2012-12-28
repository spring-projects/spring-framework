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

package org.springframework.test.util;

import static org.junit.Assert.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.test.AssertThrows;
import org.springframework.test.util.subpackage.Component;
import org.springframework.test.util.subpackage.LegacyEntity;
import org.springframework.test.util.subpackage.Person;

/**
 * Unit tests for {@link ReflectionTestUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
public class ReflectionTestUtilsTests {

	private static final Float PI = new Float((float) 22 / 7);

	private final Person person = new Person();
	private final Component component = new Component();


	@Test
	public void setAndGetFields() throws Exception {

		// ---------------------------------------------------------------------
		// Standard

		setField(person, "id", new Long(99), long.class);
		setField(person, "name", "Tom");
		setField(person, "age", new Integer(42));
		setField(person, "eyeColor", "blue", String.class);
		setField(person, "likesPets", Boolean.TRUE);
		setField(person, "favoriteNumber", PI, Number.class);

		assertEquals("ID (private field in a superclass)", 99, person.getId());
		assertEquals("name (protected field)", "Tom", person.getName());
		assertEquals("age (private field)", 42, person.getAge());
		assertEquals("eye color (package private field)", "blue", person.getEyeColor());
		assertEquals("'likes pets' flag (package private boolean field)", true, person.likesPets());
		assertEquals("'favorite number' (package field)", PI, person.getFavoriteNumber());

		assertEquals(new Long(99), getField(person, "id"));
		assertEquals("Tom", getField(person, "name"));
		assertEquals(new Integer(42), getField(person, "age"));
		assertEquals("blue", getField(person, "eyeColor"));
		assertEquals(Boolean.TRUE, getField(person, "likesPets"));
		assertEquals(PI, getField(person, "favoriteNumber"));

		// ---------------------------------------------------------------------
		// Null - non-primitives

		setField(person, "name", null, String.class);
		setField(person, "eyeColor", null, String.class);
		setField(person, "favoriteNumber", null, Number.class);

		assertNull("name (protected field)", person.getName());
		assertNull("eye color (package private field)", person.getEyeColor());
		assertNull("'favorite number' (package field)", person.getFavoriteNumber());

		// ---------------------------------------------------------------------
		// Null - primitives

		new AssertThrows(IllegalArgumentException.class,
			"Calling setField() with NULL for a primitive type should throw an IllegalArgumentException.") {

			@Override
			public void test() throws Exception {
				setField(person, "id", null, long.class);
			}
		}.runTest();

		new AssertThrows(IllegalArgumentException.class,
			"Calling setField() with NULL for a primitive type should throw an IllegalArgumentException.") {

			@Override
			public void test() throws Exception {
				setField(person, "age", null, int.class);
			}
		}.runTest();

		new AssertThrows(IllegalArgumentException.class,
			"Calling setField() with NULL for a primitive type should throw an IllegalArgumentException.") {

			@Override
			public void test() throws Exception {
				setField(person, "likesPets", null, boolean.class);
			}
		}.runTest();
	}

	/**
	 * Verifies behavior requested in <a href="https://jira.springsource.org/browse/SPR-9571">SPR-9571</a>.
	 */
	@Test
	public void setFieldOnLegacyEntityWithSideEffectsInToString() {
		String testCollaborator = "test collaborator";
		LegacyEntity entity = new LegacyEntity();
		setField(entity, "collaborator", testCollaborator, Object.class);
		assertTrue(entity.toString().contains(testCollaborator));
	}

	@Test
	public void invokeSetterAndMethods() throws Exception {

		// ---------------------------------------------------------------------
		// Standard - properties

		invokeSetterMethod(person, "id", new Long(99), long.class);
		invokeSetterMethod(person, "name", "Tom");
		invokeSetterMethod(person, "age", new Integer(42));
		invokeSetterMethod(person, "eyeColor", "blue", String.class);
		invokeSetterMethod(person, "likesPets", Boolean.TRUE);
		invokeSetterMethod(person, "favoriteNumber", PI, Number.class);

		assertEquals("ID (protected method in a superclass)", 99, person.getId());
		assertEquals("name (private method)", "Tom", person.getName());
		assertEquals("age (protected method)", 42, person.getAge());
		assertEquals("eye color (package private method)", "blue", person.getEyeColor());
		assertEquals("'likes pets' flag (protected method for a boolean)", true, person.likesPets());
		assertEquals("'favorite number' (protected method for a Number)", PI, person.getFavoriteNumber());

		assertEquals(new Long(99), invokeGetterMethod(person, "id"));
		assertEquals("Tom", invokeGetterMethod(person, "name"));
		assertEquals(new Integer(42), invokeGetterMethod(person, "age"));
		assertEquals("blue", invokeGetterMethod(person, "eyeColor"));
		assertEquals(Boolean.TRUE, invokeGetterMethod(person, "likesPets"));
		assertEquals(PI, invokeGetterMethod(person, "favoriteNumber"));

		// ---------------------------------------------------------------------
		// Standard - setter methods

		invokeSetterMethod(person, "setId", new Long(1), long.class);
		invokeSetterMethod(person, "setName", "Jerry", String.class);
		invokeSetterMethod(person, "setAge", new Integer(33), int.class);
		invokeSetterMethod(person, "setEyeColor", "green", String.class);
		invokeSetterMethod(person, "setLikesPets", Boolean.FALSE, boolean.class);
		invokeSetterMethod(person, "setFavoriteNumber", new Integer(42), Number.class);

		assertEquals("ID (protected method in a superclass)", 1, person.getId());
		assertEquals("name (private method)", "Jerry", person.getName());
		assertEquals("age (protected method)", 33, person.getAge());
		assertEquals("eye color (package private method)", "green", person.getEyeColor());
		assertEquals("'likes pets' flag (protected method for a boolean)", false, person.likesPets());
		assertEquals("'favorite number' (protected method for a Number)", new Integer(42), person.getFavoriteNumber());

		assertEquals(new Long(1), invokeGetterMethod(person, "getId"));
		assertEquals("Jerry", invokeGetterMethod(person, "getName"));
		assertEquals(new Integer(33), invokeGetterMethod(person, "getAge"));
		assertEquals("green", invokeGetterMethod(person, "getEyeColor"));
		assertEquals(Boolean.FALSE, invokeGetterMethod(person, "likesPets"));
		assertEquals(new Integer(42), invokeGetterMethod(person, "getFavoriteNumber"));

		// ---------------------------------------------------------------------
		// Null - non-primitives

		invokeSetterMethod(person, "name", null, String.class);
		invokeSetterMethod(person, "eyeColor", null, String.class);
		invokeSetterMethod(person, "favoriteNumber", null, Number.class);

		assertNull("name (private method)", person.getName());
		assertNull("eye color (package private method)", person.getEyeColor());
		assertNull("'favorite number' (protected method for a Number)", person.getFavoriteNumber());

		// ---------------------------------------------------------------------
		// Null - primitives

		new AssertThrows(IllegalArgumentException.class,
			"Calling invokeSetterMethod() with NULL for a primitive type should throw an IllegalArgumentException.") {

			@Override
			public void test() throws Exception {
				invokeSetterMethod(person, "id", null, long.class);
			}
		}.runTest();

		new AssertThrows(IllegalArgumentException.class,
			"Calling invokeSetterMethod() with NULL for a primitive type should throw an IllegalArgumentException.") {

			@Override
			public void test() throws Exception {
				invokeSetterMethod(person, "age", null, int.class);
			}
		}.runTest();

		new AssertThrows(IllegalArgumentException.class,
			"Calling invokeSetterMethod() with NULL for a primitive type should throw an IllegalArgumentException.") {

			@Override
			public void test() throws Exception {
				invokeSetterMethod(person, "likesPets", null, boolean.class);
			}
		}.runTest();
	}

	@Test
	public void invokeMethodWithAutoboxingAndUnboxing() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer difference = invokeMethod(component, "subtract", 5, 2);
		assertEquals("subtract(5, 2)", 3, difference.intValue());
	}

	@Ignore("[SPR-8644] findMethod() does not currently support var-args")
	@Test
	public void invokeMethodWithPrimitiveVarArgs() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer sum = invokeMethod(component, "add", 1, 2, 3, 4);
		assertEquals("add(1,2,3,4)", 10, sum.intValue());
	}

	@Test
	public void invokeMethodWithPrimitiveVarArgsAsSingleArgument() {
		// IntelliJ IDEA 11 won't accept int assignment here
		Integer sum = invokeMethod(component, "add", new int[] { 1, 2, 3, 4 });
		assertEquals("add(1,2,3,4)", 10, sum.intValue());
	}

	@Test
	public void invokeMethodsSimulatingLifecycleEvents() {
		assertNull("number", component.getNumber());
		assertNull("text", component.getText());

		// Simulate autowiring a configuration method
		invokeMethod(component, "configure", new Integer(42), "enigma");
		assertEquals("number should have been configured", new Integer(42), component.getNumber());
		assertEquals("text should have been configured", "enigma", component.getText());

		// Simulate @PostConstruct life-cycle event
		invokeMethod(component, "init");
		// assertions in init() should succeed

		// Simulate @PreDestroy life-cycle event
		invokeMethod(component, "destroy");
		assertNull("number", component.getNumber());
		assertNull("text", component.getText());
	}

	@Test(expected = IllegalStateException.class)
	public void invokeMethodWithIncompatibleArgumentTypes() {
		invokeMethod(component, "subtract", "foo", 2.0);
	}

	@Test(expected = IllegalStateException.class)
	public void invokeInitMethodBeforeAutowiring() {
		invokeMethod(component, "init");
	}

	@Test(expected = IllegalStateException.class)
	public void invokeMethodWithTooFewArguments() {
		invokeMethod(component, "configure", new Integer(42));
	}

	@Test(expected = IllegalStateException.class)
	public void invokeMethodWithTooManyArguments() {
		invokeMethod(component, "configure", new Integer(42), "enigma", "baz", "quux");
	}

}
