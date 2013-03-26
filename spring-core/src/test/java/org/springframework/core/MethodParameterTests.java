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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class MethodParameterTests {

	private MethodParameter stringParameter;

	private MethodParameter longParameter;

	private MethodParameter intReturnType;


	@Before
	public void setUp() throws NoSuchMethodException {
		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		stringParameter = new MethodParameter(method, 0);
		longParameter = new MethodParameter(method, 1);
		intReturnType = new MethodParameter(method, -1);
	}

	@Test
	public void testEquals() throws NoSuchMethodException {
		assertEquals(stringParameter, stringParameter);
		assertEquals(longParameter, longParameter);
		assertEquals(intReturnType, intReturnType);

		assertFalse(stringParameter.equals(longParameter));
		assertFalse(stringParameter.equals(intReturnType));
		assertFalse(longParameter.equals(stringParameter));
		assertFalse(longParameter.equals(intReturnType));
		assertFalse(intReturnType.equals(stringParameter));
		assertFalse(intReturnType.equals(longParameter));

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertEquals(stringParameter, methodParameter);
		assertEquals(methodParameter, stringParameter);
		assertFalse(longParameter.equals(methodParameter));
		assertFalse(methodParameter.equals(longParameter));
	}

	@Test
	public void testHashCode() throws NoSuchMethodException {
		assertEquals(stringParameter.hashCode(), stringParameter.hashCode());
		assertEquals(longParameter.hashCode(), longParameter.hashCode());
		assertEquals(intReturnType.hashCode(), intReturnType.hashCode());

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertEquals(stringParameter.hashCode(), methodParameter.hashCode());
		assertTrue(longParameter.hashCode() != methodParameter.hashCode());
	}


	public int method(String p1, long p2) {
		return 42;
	}

}
