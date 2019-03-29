/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.expression;

import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class AnnotatedElementKeyTests {

	@Rule
	public final TestName name = new TestName();

	@Test
	public void sameInstanceEquals() {
		Method m = ReflectionUtils.findMethod(getClass(), name.getMethodName());
		AnnotatedElementKey instance = new AnnotatedElementKey(m, getClass());
		assertKeyEquals(instance, instance);
	}

	@Test
	public void equals() {
		Method m = ReflectionUtils.findMethod(getClass(), name.getMethodName());
		AnnotatedElementKey first = new AnnotatedElementKey(m, getClass());
		AnnotatedElementKey second = new AnnotatedElementKey(m, getClass());

		assertKeyEquals(first, second);
	}

	@Test
	public void equalsNoTarget() {
		Method m = ReflectionUtils.findMethod(getClass(), name.getMethodName());
		AnnotatedElementKey first = new AnnotatedElementKey(m, null);
		AnnotatedElementKey second = new AnnotatedElementKey(m, null);

		assertKeyEquals(first, second);
	}

	@Test
	public void noTargetClassNotEquals() {
		Method m = ReflectionUtils.findMethod(getClass(), name.getMethodName());
		AnnotatedElementKey first = new AnnotatedElementKey(m, getClass());
		AnnotatedElementKey second = new AnnotatedElementKey(m, null);

		assertFalse(first.equals(second));
	}

	protected void assertKeyEquals(AnnotatedElementKey first, AnnotatedElementKey second) {
		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
	}

}
