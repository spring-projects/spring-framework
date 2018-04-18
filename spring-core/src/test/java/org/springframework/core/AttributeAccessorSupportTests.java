/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
public class AttributeAccessorSupportTests {

	private static final String NAME = "foo";

	private static final String VALUE = "bar";

	private AttributeAccessor attributeAccessor = new SimpleAttributeAccessorSupport();

	@Test
	public void setAndGet() throws Exception {
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertEquals(VALUE, this.attributeAccessor.getAttribute(NAME));
	}

	@Test
	public void setAndHas() throws Exception {
		assertFalse(this.attributeAccessor.hasAttribute(NAME));
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertTrue(this.attributeAccessor.hasAttribute(NAME));
	}

	@Test
	public void remove() throws Exception {
		assertFalse(this.attributeAccessor.hasAttribute(NAME));
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertEquals(VALUE, this.attributeAccessor.removeAttribute(NAME));
		assertFalse(this.attributeAccessor.hasAttribute(NAME));
	}

	@Test
	public void attributeNames() throws Exception {
		this.attributeAccessor.setAttribute(NAME, VALUE);
		this.attributeAccessor.setAttribute("abc", "123");
		String[] attributeNames = this.attributeAccessor.attributeNames();
		Arrays.sort(attributeNames);
		assertTrue(Arrays.binarySearch(attributeNames, NAME) > -1);
		assertTrue(Arrays.binarySearch(attributeNames, "abc") > -1);
	}

	@SuppressWarnings("serial")
	private static class SimpleAttributeAccessorSupport extends AttributeAccessorSupport {
	}

}
