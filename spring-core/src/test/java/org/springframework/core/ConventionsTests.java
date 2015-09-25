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

package org.springframework.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.tests.sample.objects.TestObject;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 */
public class ConventionsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void simpleObject() {
		assertEquals("Incorrect singular variable name", "testObject", Conventions.getVariableName(new TestObject()));
	}

	@Test
	public void array() {
		assertEquals("Incorrect plural array form", "testObjectList", Conventions.getVariableName(new TestObject[0]));
	}

	@Test
	public void list() {
		List<TestObject> list = Arrays.asList(new TestObject());
		assertEquals("Incorrect plural List form", "testObjectList", Conventions.getVariableName(list));
	}

	@Test
	public void emptyList() {
		exception.expect(IllegalArgumentException.class);
		Conventions.getVariableName(new ArrayList<>());
	}

	@Test
	public void set() {
		assertEquals("Incorrect plural Set form", "testObjectList", Conventions.getVariableName(Collections.singleton(new TestObject())));
	}

	@Test
	public void attributeNameToPropertyName() throws Exception {
		assertEquals("transactionManager", Conventions.attributeNameToPropertyName("transaction-manager"));
		assertEquals("pointcutRef", Conventions.attributeNameToPropertyName("pointcut-ref"));
		assertEquals("lookupOnStartup", Conventions.attributeNameToPropertyName("lookup-on-startup"));
	}

	@Test
	public void getQualifiedAttributeName() throws Exception {
		String baseName = "foo";
		Class<String> cls = String.class;
		String desiredResult = "java.lang.String.foo";
		assertEquals(desiredResult, Conventions.getQualifiedAttributeName(cls, baseName));
	}

}
