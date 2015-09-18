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

package org.springframework.context.expression;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MethodBasedEvaluationContext}.
 *
 * @author Stephane Nicoll
 */
public class MethodBasedEvaluationContextTests {

	private final ParameterNameDiscoverer paramDiscover = new DefaultParameterNameDiscoverer();

	@Test
	public void simpleArguments() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello",
				String.class, Boolean.class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, new Object[] {"test", true});

		assertEquals("test", context.lookupVariable("a0"));
		assertEquals("test", context.lookupVariable("p0"));
		assertEquals("test", context.lookupVariable("foo"));

		assertEquals(true, context.lookupVariable("a1"));
		assertEquals(true, context.lookupVariable("p1"));
		assertEquals(true, context.lookupVariable("flag"));

		assertNull(context.lookupVariable("a2"));
	}

	@Test
	public void nullArgument() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello",
				String.class, Boolean.class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, new Object[] {null, null});

		assertNull(context.lookupVariable("a0"));
		assertNull(context.lookupVariable("p0"));
	}

	private MethodBasedEvaluationContext createEvaluationContext(Method method, Object[] args) {
		return new MethodBasedEvaluationContext(this, method, args, this.paramDiscover);
	}


	@SuppressWarnings("unused")
	private static class SampleMethods {

		private void hello(String foo, Boolean flag) {
		}

	}

}