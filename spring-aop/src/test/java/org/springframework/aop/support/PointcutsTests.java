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

package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class PointcutsTests {

	public static Method TEST_BEAN_SET_AGE;
	public static Method TEST_BEAN_GET_AGE;
	public static Method TEST_BEAN_GET_NAME;
	public static Method TEST_BEAN_ABSQUATULATE;

	static {
		try {
			TEST_BEAN_SET_AGE = TestBean.class.getMethod("setAge", int.class);
			TEST_BEAN_GET_AGE = TestBean.class.getMethod("getAge");
			TEST_BEAN_GET_NAME = TestBean.class.getMethod("getName");
			TEST_BEAN_ABSQUATULATE = TestBean.class.getMethod("absquatulate");
		}
		catch (Exception ex) {
			throw new RuntimeException("Shouldn't happen: error in test suite");
		}
	}

	/**
	 * Matches only TestBean class, not subclasses
	 */
	public static Pointcut allTestBeanMethodsPointcut = new StaticMethodMatcherPointcut() {
		@Override
		public ClassFilter getClassFilter() {
			return new ClassFilter() {
				@Override
				public boolean matches(Class<?> clazz) {
					return clazz.equals(TestBean.class);
				}
			};
		}

		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return true;
		}
	};

	public static Pointcut allClassSetterPointcut = Pointcuts.SETTERS;

	// Subclass used for matching
	public static class MyTestBean extends TestBean {
	}

	public static Pointcut myTestBeanSetterPointcut = new StaticMethodMatcherPointcut() {
		@Override
		public ClassFilter getClassFilter() {
			return new RootClassFilter(MyTestBean.class);
		}

		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().startsWith("set");
		}
	};

	// Will match MyTestBeanSubclass
	public static Pointcut myTestBeanGetterPointcut = new StaticMethodMatcherPointcut() {
		@Override
		public ClassFilter getClassFilter() {
			return new RootClassFilter(MyTestBean.class);
		}

		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().startsWith("get");
		}
	};

	// Still more specific class
	public static class MyTestBeanSubclass extends MyTestBean {
	}

	public static Pointcut myTestBeanSubclassGetterPointcut = new StaticMethodMatcherPointcut() {
		@Override
		public ClassFilter getClassFilter() {
			return new RootClassFilter(MyTestBeanSubclass.class);
		}

		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().startsWith("get");
		}
	};

	public static Pointcut allClassGetterPointcut = Pointcuts.GETTERS;

	public static Pointcut allClassGetAgePointcut = new NameMatchMethodPointcut().addMethodName("getAge");

	public static Pointcut allClassGetNamePointcut = new NameMatchMethodPointcut().addMethodName("getName");


	@Test
	public void testTrue() {
		assertTrue(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertTrue(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_GET_AGE, TestBean.class));
		assertTrue(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_ABSQUATULATE, TestBean.class));
		assertTrue(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertTrue(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_GET_AGE, TestBean.class));
		assertTrue(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_ABSQUATULATE, TestBean.class));
	}

	@Test
	public void testMatches() {
		assertTrue(Pointcuts.matches(allClassSetterPointcut, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertFalse(Pointcuts.matches(allClassSetterPointcut, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(allClassSetterPointcut, TEST_BEAN_ABSQUATULATE, TestBean.class));
		assertFalse(Pointcuts.matches(allClassGetterPointcut, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertTrue(Pointcuts.matches(allClassGetterPointcut, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(allClassGetterPointcut, TEST_BEAN_ABSQUATULATE, TestBean.class));
	}

	/**
	 * Should match all setters and getters on any class
	 */
	@Test
	public void testUnionOfSettersAndGetters() {
		Pointcut union = Pointcuts.union(allClassGetterPointcut, allClassSetterPointcut);
		assertTrue(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class));
	}

	@Test
	public void testUnionOfSpecificGetters() {
		Pointcut union = Pointcuts.union(allClassGetAgePointcut, allClassGetNamePointcut);
		assertFalse(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_NAME, TestBean.class));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_NAME, TestBean.class));
		assertFalse(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class));

		// Union with all setters
		union = Pointcuts.union(union, allClassSetterPointcut);
		assertTrue(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_NAME, TestBean.class));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_NAME, TestBean.class));
		assertFalse(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class));

		assertTrue(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
	}

	/**
	 * Tests vertical composition. First pointcut matches all setters.
	 * Second one matches all getters in the MyTestBean class. TestBean getters shouldn't pass.
	 */
	@Test
	public void testUnionOfAllSettersAndSubclassSetters() {
		assertFalse(Pointcuts.matches(myTestBeanSetterPointcut, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertTrue(Pointcuts.matches(myTestBeanSetterPointcut, TEST_BEAN_SET_AGE, MyTestBean.class, new Integer(6)));
		assertFalse(Pointcuts.matches(myTestBeanSetterPointcut, TEST_BEAN_GET_AGE, TestBean.class));

		Pointcut union = Pointcuts.union(myTestBeanSetterPointcut, allClassGetterPointcut);
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_AGE, MyTestBean.class));
		// Still doesn't match superclass setter
		assertTrue(Pointcuts.matches(union, TEST_BEAN_SET_AGE, MyTestBean.class, new Integer(6)));
		assertFalse(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
	}

	/**
	 * Intersection should be MyTestBean getAge() only:
	 * it's the union of allClassGetAge and subclass getters
	 */
	@Test
	public void testIntersectionOfSpecificGettersAndSubclassGetters() {
		assertTrue(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_AGE, TestBean.class));
		assertTrue(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_AGE, MyTestBean.class));
		assertFalse(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_NAME, TestBean.class));
		assertFalse(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_AGE, TestBean.class));
		assertTrue(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_NAME, MyTestBean.class));
		assertTrue(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_AGE, MyTestBean.class));

		Pointcut intersection = Pointcuts.intersection(allClassGetAgePointcut, myTestBeanGetterPointcut);
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, TestBean.class));
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBean.class));
		assertTrue(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBean.class));
		// Matches subclass of MyTestBean
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBeanSubclass.class));
		assertTrue(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBeanSubclass.class));

		// Now intersection with MyTestBeanSubclass getters should eliminate MyTestBean target
		intersection = Pointcuts.intersection(intersection, myTestBeanSubclassGetterPointcut);
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, TestBean.class));
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBean.class));
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBean.class));
		// Still matches subclass of MyTestBean
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBeanSubclass.class));
		assertTrue(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBeanSubclass.class));

		// Now union with all TestBean methods
		Pointcut union = Pointcuts.union(intersection, allTestBeanMethodsPointcut);
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_NAME, TestBean.class));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(union, TEST_BEAN_GET_NAME, MyTestBean.class));
		assertFalse(Pointcuts.matches(union, TEST_BEAN_GET_AGE, MyTestBean.class));
		// Still matches subclass of MyTestBean
		assertFalse(Pointcuts.matches(union, TEST_BEAN_GET_NAME, MyTestBeanSubclass.class));
		assertTrue(Pointcuts.matches(union, TEST_BEAN_GET_AGE, MyTestBeanSubclass.class));

		assertTrue(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class));
		assertFalse(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, MyTestBean.class));
	}


	/**
	 * The intersection of these two pointcuts leaves nothing.
	 */
	@Test
	public void testSimpleIntersection() {
		Pointcut intersection = Pointcuts.intersection(allClassGetterPointcut, allClassSetterPointcut);
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_SET_AGE, TestBean.class, new Integer(6)));
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, TestBean.class));
		assertFalse(Pointcuts.matches(intersection, TEST_BEAN_ABSQUATULATE, TestBean.class));
	}

}
