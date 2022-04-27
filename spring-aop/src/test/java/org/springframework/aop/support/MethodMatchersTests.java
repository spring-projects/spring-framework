/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aop.MethodMatcher;
import org.springframework.beans.testfixture.beans.IOther;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class MethodMatchersTests {

	private final Method EXCEPTION_GETMESSAGE;

	private final Method ITESTBEAN_SETAGE;

	private final Method ITESTBEAN_GETAGE;

	private final Method IOTHER_ABSQUATULATE;


	public MethodMatchersTests() throws Exception {
		EXCEPTION_GETMESSAGE = Exception.class.getMethod("getMessage");
		ITESTBEAN_GETAGE = ITestBean.class.getMethod("getAge");
		ITESTBEAN_SETAGE = ITestBean.class.getMethod("setAge", int.class);
		IOTHER_ABSQUATULATE = IOther.class.getMethod("absquatulate");
	}


	@Test
	public void testDefaultMatchesAll() throws Exception {
		MethodMatcher defaultMm = MethodMatcher.TRUE;
		assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
		assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isTrue();
	}

	@Test
	public void testMethodMatcherTrueSerializable() throws Exception {
		assertThat(MethodMatcher.TRUE).isSameAs(SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE));
	}

	@Test
	public void testSingle() throws Exception {
		MethodMatcher defaultMm = MethodMatcher.TRUE;
		assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
		assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isTrue();
		defaultMm = MethodMatchers.intersection(defaultMm, new StartsWithMatcher("get"));

		assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
		assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isFalse();
	}


	@Test
	public void testDynamicAndStaticMethodMatcherIntersection() throws Exception {
		MethodMatcher mm1 = MethodMatcher.TRUE;
		MethodMatcher mm2 = new TestDynamicMethodMatcherWhichMatches();
		MethodMatcher intersection = MethodMatchers.intersection(mm1, mm2);
		assertThat(intersection.isRuntime()).as("Intersection is a dynamic matcher").isTrue();
		assertThat(intersection.matches(ITESTBEAN_SETAGE, TestBean.class)).as("2Matched setAge method").isTrue();
		assertThat(intersection.matches(ITESTBEAN_SETAGE, TestBean.class, 5)).as("3Matched setAge method").isTrue();
		// Knock out dynamic part
		intersection = MethodMatchers.intersection(intersection, new TestDynamicMethodMatcherWhichDoesNotMatch());
		assertThat(intersection.isRuntime()).as("Intersection is a dynamic matcher").isTrue();
		assertThat(intersection.matches(ITESTBEAN_SETAGE, TestBean.class)).as("2Matched setAge method").isTrue();
		assertThat(intersection.matches(ITESTBEAN_SETAGE, TestBean.class, 5)).as("3 - not Matched setAge method").isFalse();
	}

	@Test
	public void testStaticMethodMatcherUnion() throws Exception {
		MethodMatcher getterMatcher = new StartsWithMatcher("get");
		MethodMatcher setterMatcher = new StartsWithMatcher("set");
		MethodMatcher union = MethodMatchers.union(getterMatcher, setterMatcher);

		assertThat(union.isRuntime()).as("Union is a static matcher").isFalse();
		assertThat(union.matches(ITESTBEAN_SETAGE, TestBean.class)).as("Matched setAge method").isTrue();
		assertThat(union.matches(ITESTBEAN_GETAGE, TestBean.class)).as("Matched getAge method").isTrue();
		assertThat(union.matches(IOTHER_ABSQUATULATE, TestBean.class)).as("Didn't matched absquatulate method").isFalse();
	}

	@Test
	public void testUnionEquals() {
		MethodMatcher first = MethodMatchers.union(MethodMatcher.TRUE, MethodMatcher.TRUE);
		MethodMatcher second = new ComposablePointcut(MethodMatcher.TRUE).union(new ComposablePointcut(MethodMatcher.TRUE)).getMethodMatcher();
		assertThat(first.equals(second)).isTrue();
		assertThat(second.equals(first)).isTrue();
	}


	public static class StartsWithMatcher extends StaticMethodMatcher {

		private final String prefix;

		public StartsWithMatcher(String s) {
			this.prefix = s;
		}

		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().startsWith(prefix);
		}
	}


	private static class TestDynamicMethodMatcherWhichMatches extends DynamicMethodMatcher {

		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass, Object... args) {
			return true;
		}
	}


	private static class TestDynamicMethodMatcherWhichDoesNotMatch extends DynamicMethodMatcher {

		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass, Object... args) {
			return false;
		}
	}

}
