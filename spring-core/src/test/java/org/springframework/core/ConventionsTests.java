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

package org.springframework.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.springframework.tests.sample.objects.TestObject;

/**
 * @author Rob Harrop
 */
public class ConventionsTests extends TestCase {

	public void testSimpleObject() {
		TestObject testObject = new TestObject();
		assertEquals("Incorrect singular variable name", "testObject", Conventions.getVariableName(testObject));
	}

	public void testArray() {
		TestObject[] testObjects = new TestObject[0];
		assertEquals("Incorrect plural array form", "testObjectList", Conventions.getVariableName(testObjects));
	}

	public void testCollections() {
		List<TestObject> list = new ArrayList<TestObject>();
		list.add(new TestObject());
		assertEquals("Incorrect plural List form", "testObjectList", Conventions.getVariableName(list));

		Set<TestObject> set = new HashSet<TestObject>();
		set.add(new TestObject());
		assertEquals("Incorrect plural Set form", "testObjectList", Conventions.getVariableName(set));

		List<?> emptyList = new ArrayList<Object>();
		try {
			Conventions.getVariableName(emptyList);
			fail("Should not be able to generate name for empty collection");
		}
		catch(IllegalArgumentException ex) {
			// success
		}
	}

	public void testAttributeNameToPropertyName() throws Exception {
		assertEquals("transactionManager", Conventions.attributeNameToPropertyName("transaction-manager"));
		assertEquals("pointcutRef", Conventions.attributeNameToPropertyName("pointcut-ref"));
		assertEquals("lookupOnStartup", Conventions.attributeNameToPropertyName("lookup-on-startup"));
	}

	public void testGetQualifiedAttributeName() throws Exception {
		String baseName = "foo";
		Class<String> cls = String.class;
		String desiredResult = "java.lang.String.foo";
		assertEquals(desiredResult, Conventions.getQualifiedAttributeName(cls, baseName));
	}
}
