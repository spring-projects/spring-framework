/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.support;

import org.junit.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.tests.sample.beans.ITestBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RootClassFilter}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
public class RootClassFilterTests {

	private final ClassFilter filter1 = new RootClassFilter(Exception.class);
	private final ClassFilter filter2 = new RootClassFilter(Exception.class);
	private final ClassFilter filter3 = new RootClassFilter(ITestBean.class);

	@Test
	public void matches() {
		assertTrue(filter1.matches(Exception.class));
		assertTrue(filter1.matches(RuntimeException.class));
		assertFalse(filter1.matches(Error.class));
	}

	@Test
	public void testEquals() {
		assertEquals(filter1, filter2);
		assertNotEquals(filter1, filter3);
	}

	@Test
	public void testHashCode() {
		assertEquals(filter1.hashCode(), filter2.hashCode());
		assertNotEquals(filter1.hashCode(), filter3.hashCode());
	}

	@Test
	public void testToString() {
		assertEquals("org.springframework.aop.support.RootClassFilter: java.lang.Exception", filter1.toString());
		assertEquals(filter1.toString(), filter2.toString());
	}

}
