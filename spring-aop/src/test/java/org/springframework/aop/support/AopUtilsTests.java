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

package org.springframework.aop.support;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.target.EmptyTargetSource;

import test.aop.NopInterceptor;
import test.beans.TestBean;
import test.util.SerializationTestUtils;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class AopUtilsTests {

	@Test
	public void testPointcutCanNeverApply() {
		class TestPointcut extends StaticMethodMatcherPointcut {
			public boolean matches(Method method, Class<?> clazzy) {
				return false;
			}
		}

		Pointcut no = new TestPointcut();
		assertFalse(AopUtils.canApply(no, Object.class));
	}

	@Test
	public void testPointcutAlwaysApplies() {
		assertTrue(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), Object.class));
		assertTrue(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), TestBean.class));
	}

	@Test
	public void testPointcutAppliesToOneMethodOnObject() {
		class TestPointcut extends StaticMethodMatcherPointcut {
			public boolean matches(Method method, Class<?> clazz) {
				return method.getName().equals("hashCode");
			}
		}

		Pointcut pc = new TestPointcut();

		// will return true if we're not proxying interfaces
		assertTrue(AopUtils.canApply(pc, Object.class));
	}

	/**
	 * Test that when we serialize and deserialize various canonical instances
	 * of AOP classes, they return the same instance, not a new instance
	 * that's subverted the singleton construction limitation.
	 */
	@Test
	public void testCanonicalFrameworkClassesStillCanonicalOnDeserialization() throws Exception {
		assertSame(MethodMatcher.TRUE, SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE));
		assertSame(ClassFilter.TRUE, SerializationTestUtils.serializeAndDeserialize(ClassFilter.TRUE));
		assertSame(Pointcut.TRUE, SerializationTestUtils.serializeAndDeserialize(Pointcut.TRUE));
		assertSame(EmptyTargetSource.INSTANCE, SerializationTestUtils.serializeAndDeserialize(EmptyTargetSource.INSTANCE));
		assertSame(Pointcuts.SETTERS, SerializationTestUtils.serializeAndDeserialize(Pointcuts.SETTERS));
		assertSame(Pointcuts.GETTERS, SerializationTestUtils.serializeAndDeserialize(Pointcuts.GETTERS));
		assertSame(ExposeInvocationInterceptor.INSTANCE,
				SerializationTestUtils.serializeAndDeserialize(ExposeInvocationInterceptor.INSTANCE));
	}

}
