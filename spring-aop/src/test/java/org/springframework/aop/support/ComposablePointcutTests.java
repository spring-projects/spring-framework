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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ComposablePointcutTests {

	public static MethodMatcher GETTER_METHOD_MATCHER = new StaticMethodMatcher() {
		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().startsWith("get");
		}
	};

	public static MethodMatcher GET_AGE_METHOD_MATCHER = new StaticMethodMatcher() {
		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().equals("getAge");
		}
	};

	public static MethodMatcher ABSQUATULATE_METHOD_MATCHER = new StaticMethodMatcher() {
		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().equals("absquatulate");
		}
	};

	public static MethodMatcher SETTER_METHOD_MATCHER = new StaticMethodMatcher() {
		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().startsWith("set");
		}
	};


	@Test
	public void testMatchAll() throws NoSuchMethodException {
		Pointcut pc = new ComposablePointcut();
		assertThat(pc.getClassFilter().matches(Object.class)).isTrue();
		assertThat(pc.getMethodMatcher().matches(Object.class.getMethod("hashCode"), Exception.class)).isTrue();
	}

	@Test
	public void testFilterByClass() throws NoSuchMethodException {
		ComposablePointcut pc = new ComposablePointcut();

		assertThat(pc.getClassFilter().matches(Object.class)).isTrue();

		ClassFilter cf = new RootClassFilter(Exception.class);
		pc.intersection(cf);
		assertThat(pc.getClassFilter().matches(Object.class)).isFalse();
		assertThat(pc.getClassFilter().matches(Exception.class)).isTrue();
		pc.intersection(new RootClassFilter(NestedRuntimeException.class));
		assertThat(pc.getClassFilter().matches(Exception.class)).isFalse();
		assertThat(pc.getClassFilter().matches(NestedRuntimeException.class)).isTrue();
		assertThat(pc.getClassFilter().matches(String.class)).isFalse();
		pc.union(new RootClassFilter(String.class));
		assertThat(pc.getClassFilter().matches(Exception.class)).isFalse();
		assertThat(pc.getClassFilter().matches(String.class)).isTrue();
		assertThat(pc.getClassFilter().matches(NestedRuntimeException.class)).isTrue();
	}

	@Test
	public void testUnionMethodMatcher() {
		// Matches the getAge() method in any class
		ComposablePointcut pc = new ComposablePointcut(ClassFilter.TRUE, GET_AGE_METHOD_MATCHER);
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isFalse();

		pc.union(GETTER_METHOD_MATCHER);
		// Should now match all getter methods
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();

		pc.union(ABSQUATULATE_METHOD_MATCHER);
		// Should now match absquatulate() as well
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isTrue();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
		// But it doesn't match everything
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_SET_AGE, TestBean.class)).isFalse();
	}

	@Test
	public void testIntersectionMethodMatcher() {
		ComposablePointcut pc = new ComposablePointcut();
		assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isTrue();
		assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
		assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
		pc.intersection(GETTER_METHOD_MATCHER);
		assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
		assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
		assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
		pc.intersection(GET_AGE_METHOD_MATCHER);
		// Use the Pointcuts matches method
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
		assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isFalse();
	}

	@Test
	public void testEqualsAndHashCode() throws Exception {
		ComposablePointcut pc1 = new ComposablePointcut();
		ComposablePointcut pc2 = new ComposablePointcut();

		assertThat(pc2).isEqualTo(pc1);
		assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());

		pc1.intersection(GETTER_METHOD_MATCHER);

		assertThat(pc1.equals(pc2)).isFalse();
		assertThat(pc1.hashCode() == pc2.hashCode()).isFalse();

		pc2.intersection(GETTER_METHOD_MATCHER);

		assertThat(pc2).isEqualTo(pc1);
		assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());

		pc1.union(GET_AGE_METHOD_MATCHER);
		pc2.union(GET_AGE_METHOD_MATCHER);

		assertThat(pc2).isEqualTo(pc1);
		assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());
	}

}
