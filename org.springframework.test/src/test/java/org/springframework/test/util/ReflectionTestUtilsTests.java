/*
 * Copyright 2002-2008 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.test.AssertThrows;
import org.springframework.test.util.subpackage.Person;

/**
 * JUnit 3.8 based unit tests for {@link ReflectionTestUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
public class ReflectionTestUtilsTests extends TestCase {

	protected static final Float PI = new Float((float) 22 / 7);


	public void testSetField() throws Exception {
		final Person person = new Person();

		// Standard

		ReflectionTestUtils.setField(person, "id", new Long(99), long.class);
		ReflectionTestUtils.setField(person, "name", "Tom");
		ReflectionTestUtils.setField(person, "age", new Integer(42));
		ReflectionTestUtils.setField(person, "eyeColor", "blue", String.class);
		ReflectionTestUtils.setField(person, "likesPets", Boolean.TRUE);
		ReflectionTestUtils.setField(person, "favoriteNumber", PI, Number.class);

		assertEquals("Verifying that the person's ID (private field in a superclass) was set.", 99, person.getId());
		assertEquals("Verifying that the person's name (protected field) was set.", "Tom", person.getName());
		assertEquals("Verifying that the person's age (private field) was set.", 42, person.getAge());
		assertEquals("Verifying that the person's eye color (package private field) was set.", "blue",
				person.getEyeColor());
		assertEquals("Verifying that the person's 'likes pets' flag (package private boolean field) was set.", true,
				person.likesPets());
		assertEquals("Verifying that the person's 'favorite number' (package field) was set.", PI,
				person.getFavoriteNumber());

		assertEquals(new Long(99), ReflectionTestUtils.getField(person, "id"));
		assertEquals("Tom", ReflectionTestUtils.getField(person, "name"));
		assertEquals(new Integer(42), ReflectionTestUtils.getField(person, "age"));
		assertEquals("blue", ReflectionTestUtils.getField(person, "eyeColor"));
		assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(person, "likesPets"));
		assertEquals(PI, ReflectionTestUtils.getField(person, "favoriteNumber"));

		// Null - non-primitives

		ReflectionTestUtils.setField(person, "name", null, String.class);
		ReflectionTestUtils.setField(person, "eyeColor", null, String.class);
		ReflectionTestUtils.setField(person, "favoriteNumber", null, Number.class);

		assertNull("Verifying that the person's name (protected field) was set.", person.getName());
		assertNull("Verifying that the person's eye color (package private field) was set.", person.getEyeColor());
		assertNull("Verifying that the person's 'favorite number' (package field) was set.", person.getFavoriteNumber());

		// Null - primitives

		new AssertThrows(IllegalArgumentException.class,
				"Calling setField() with NULL for a primitive type should throw an IllegalArgumentException.") {

			public void test() throws Exception {
				ReflectionTestUtils.setField(person, "id", null, long.class);
			}
		}.runTest();

		new AssertThrows(IllegalArgumentException.class,
				"Calling setField() with NULL for a primitive type should throw an IllegalArgumentException.") {

			public void test() throws Exception {
				ReflectionTestUtils.setField(person, "age", null, int.class);
			}
		}.runTest();

		new AssertThrows(IllegalArgumentException.class,
				"Calling setField() with NULL for a primitive type should throw an IllegalArgumentException.") {

			public void test() throws Exception {
				ReflectionTestUtils.setField(person, "likesPets", null, boolean.class);
			}
		}.runTest();
	}

	public void testInvokeSetterMethod() throws Exception {
		final Person person = new Person();

		// Standard - properties

		ReflectionTestUtils.invokeSetterMethod(person, "id", new Long(99), long.class);
		ReflectionTestUtils.invokeSetterMethod(person, "name", "Tom");
		ReflectionTestUtils.invokeSetterMethod(person, "age", new Integer(42));
		ReflectionTestUtils.invokeSetterMethod(person, "eyeColor", "blue", String.class);
		ReflectionTestUtils.invokeSetterMethod(person, "likesPets", Boolean.TRUE);
		ReflectionTestUtils.invokeSetterMethod(person, "favoriteNumber", PI, Number.class);

		assertEquals("Verifying that the person's ID (protected method in a superclass) was set.", 99, person.getId());
		assertEquals("Verifying that the person's name (private method) was set.", "Tom", person.getName());
		assertEquals("Verifying that the person's age (protected method) was set.", 42, person.getAge());
		assertEquals("Verifying that the person's eye color (package private method) was set.", "blue",
				person.getEyeColor());
		assertEquals("Verifying that the person's 'likes pets' flag (protected method for a boolean) was set.", true,
				person.likesPets());
		assertEquals("Verifying that the person's 'favorite number' (protected method for a Number) was set.", PI,
				person.getFavoriteNumber());

		assertEquals(new Long(99), ReflectionTestUtils.invokeGetterMethod(person, "id"));
		assertEquals("Tom", ReflectionTestUtils.invokeGetterMethod(person, "name"));
		assertEquals(new Integer(42), ReflectionTestUtils.invokeGetterMethod(person, "age"));
		assertEquals("blue", ReflectionTestUtils.invokeGetterMethod(person, "eyeColor"));
		assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeGetterMethod(person, "likesPets"));
		assertEquals(PI, ReflectionTestUtils.invokeGetterMethod(person, "favoriteNumber"));

		// Standard - setter methods

		ReflectionTestUtils.invokeSetterMethod(person, "setId", new Long(1), long.class);
		ReflectionTestUtils.invokeSetterMethod(person, "setName", "Jerry", String.class);
		ReflectionTestUtils.invokeSetterMethod(person, "setAge", new Integer(33), int.class);
		ReflectionTestUtils.invokeSetterMethod(person, "setEyeColor", "green", String.class);
		ReflectionTestUtils.invokeSetterMethod(person, "setLikesPets", Boolean.FALSE, boolean.class);
		ReflectionTestUtils.invokeSetterMethod(person, "setFavoriteNumber", new Integer(42), Number.class);

		assertEquals("Verifying that the person's ID (protected method in a superclass) was set.", 1, person.getId());
		assertEquals("Verifying that the person's name (private method) was set.", "Jerry", person.getName());
		assertEquals("Verifying that the person's age (protected method) was set.", 33, person.getAge());
		assertEquals("Verifying that the person's eye color (package private method) was set.", "green",
				person.getEyeColor());
		assertEquals("Verifying that the person's 'likes pets' flag (protected method for a boolean) was set.", false,
				person.likesPets());
		assertEquals("Verifying that the person's 'favorite number' (protected method for a Number) was set.",
				new Integer(42), person.getFavoriteNumber());

		assertEquals(new Long(1), ReflectionTestUtils.invokeGetterMethod(person, "getId"));
		assertEquals("Jerry", ReflectionTestUtils.invokeGetterMethod(person, "getName"));
		assertEquals(new Integer(33), ReflectionTestUtils.invokeGetterMethod(person, "getAge"));
		assertEquals("green", ReflectionTestUtils.invokeGetterMethod(person, "getEyeColor"));
		assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeGetterMethod(person, "likesPets"));
		assertEquals(new Integer(42), ReflectionTestUtils.invokeGetterMethod(person, "getFavoriteNumber"));

		// Null - non-primitives

		ReflectionTestUtils.invokeSetterMethod(person, "name", null, String.class);
		ReflectionTestUtils.invokeSetterMethod(person, "eyeColor", null, String.class);
		ReflectionTestUtils.invokeSetterMethod(person, "favoriteNumber", null, Number.class);

		assertNull("Verifying that the person's name (private method) was set.", person.getName());
		assertNull("Verifying that the person's eye color (package private method) was set.", person.getEyeColor());
		assertNull("Verifying that the person's 'favorite number' (protected method for a Number) was set.",
				person.getFavoriteNumber());

		// Null - primitives

		new AssertThrows(RuntimeException.class,
				"Calling invokeSetterMethod() with NULL for a primitive type should throw an IllegalArgumentException.") {

			public void test() throws Exception {
				ReflectionTestUtils.invokeSetterMethod(person, "id", null, long.class);
			}
		}.runTest();

		new AssertThrows(RuntimeException.class,
				"Calling invokeSetterMethod() with NULL for a primitive type should throw an IllegalArgumentException.") {

			public void test() throws Exception {
				ReflectionTestUtils.invokeSetterMethod(person, "age", null, int.class);
			}
		}.runTest();

		new AssertThrows(RuntimeException.class,
				"Calling invokeSetterMethod() with NULL for a primitive type should throw an IllegalArgumentException.") {

			public void test() throws Exception {
				ReflectionTestUtils.invokeSetterMethod(person, "likesPets", null, boolean.class);
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
