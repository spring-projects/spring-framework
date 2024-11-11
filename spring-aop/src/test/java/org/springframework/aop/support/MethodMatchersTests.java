/*
 * Copyright 2002-2024 the original author or authors.
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
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class MethodMatchersTests {

	private static final Method TEST_METHOD = mock(Method.class);

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
	void testDefaultMatchesAll() {
		MethodMatcher defaultMm = MethodMatcher.TRUE;
		assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
		assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isTrue();
	}

	@Test
	void testMethodMatcherTrueSerializable() throws Exception {
		assertThat(MethodMatcher.TRUE).isSameAs(SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE));
	}

	@Test
	void testSingle() {
		MethodMatcher defaultMm = MethodMatcher.TRUE;
		assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
		assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isTrue();
		defaultMm = MethodMatchers.intersection(defaultMm, new StartsWithMatcher("get"));

		assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
		assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isFalse();
	}


	@Test
	void testDynamicAndStaticMethodMatcherIntersection() {
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
	void testStaticMethodMatcherUnion() {
		MethodMatcher getterMatcher = new StartsWithMatcher("get");
		MethodMatcher setterMatcher = new StartsWithMatcher("set");
		MethodMatcher union = MethodMatchers.union(getterMatcher, setterMatcher);

		assertThat(union.isRuntime()).as("Union is a static matcher").isFalse();
		assertThat(union.matches(ITESTBEAN_SETAGE, TestBean.class)).as("Matched setAge method").isTrue();
		assertThat(union.matches(ITESTBEAN_GETAGE, TestBean.class)).as("Matched getAge method").isTrue();
		assertThat(union.matches(IOTHER_ABSQUATULATE, TestBean.class)).as("Didn't matched absquatulate method").isFalse();
	}

	@Test
	void testUnionEquals() {
		MethodMatcher first = MethodMatchers.union(MethodMatcher.TRUE, MethodMatcher.TRUE);
		MethodMatcher second = new ComposablePointcut(MethodMatcher.TRUE).union(new ComposablePointcut(MethodMatcher.TRUE)).getMethodMatcher();
		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	@Test
	void negateMethodMatcher() {
		MethodMatcher getterMatcher = new StartsWithMatcher("get");
		MethodMatcher negate = MethodMatchers.negate(getterMatcher);
		assertThat(negate.matches(ITESTBEAN_SETAGE, int.class)).isTrue();
	}

	@Test
	void negateTrueMethodMatcher() {
		MethodMatcher negate = MethodMatchers.negate(MethodMatcher.TRUE);
		assertThat(negate.matches(TEST_METHOD, String.class)).isFalse();
		assertThat(negate.matches(TEST_METHOD, Object.class)).isFalse();
		assertThat(negate.matches(TEST_METHOD, Integer.class)).isFalse();
	}

	@Test
	void negateTrueMethodMatcherAppliedTwice() {
		MethodMatcher negate = MethodMatchers.negate(MethodMatchers.negate(MethodMatcher.TRUE));
		assertThat(negate.matches(TEST_METHOD, String.class)).isTrue();
		assertThat(negate.matches(TEST_METHOD, Object.class)).isTrue();
		assertThat(negate.matches(TEST_METHOD, Integer.class)).isTrue();
	}

	@Test
	void negateIsNotEqualsToOriginalMatcher() {
		MethodMatcher original = MethodMatcher.TRUE;
		MethodMatcher negate = MethodMatchers.negate(original);
		assertThat(original).isNotEqualTo(negate);
	}

	@Test
	void negateOnSameMatcherIsEquals() {
		MethodMatcher original = MethodMatcher.TRUE;
		MethodMatcher first = MethodMatchers.negate(original);
		MethodMatcher second = MethodMatchers.negate(original);
		assertThat(first).isEqualTo(second);
	}

	@Test
	void negateHasNotSameHashCodeAsOriginalMatcher() {
		MethodMatcher original = MethodMatcher.TRUE;
		MethodMatcher negate = MethodMatchers.negate(original);
		assertThat(original).doesNotHaveSameHashCodeAs(negate);
	}

	@Test
	void negateOnSameMatcherHasSameHashCode() {
		MethodMatcher original = MethodMatcher.TRUE;
		MethodMatcher first = MethodMatchers.negate(original);
		MethodMatcher second = MethodMatchers.negate(original);
		assertThat(first).hasSameHashCodeAs(second);
	}

	@Test
	void toStringIncludesRepresentationOfOriginalMatcher() {
		MethodMatcher original = MethodMatcher.TRUE;
		assertThat(MethodMatchers.negate(original)).hasToString("Negate " + original);
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
