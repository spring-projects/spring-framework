/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.aspectj;

import junit.framework.TestCase;
import org.springframework.aop.framework.autoproxy.CountingTestBean;
import org.springframework.beans.IOther;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.AssertThrows;

/**
 * Unit tests for the {@link TypePatternClassFilter} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 */
public final class TypePatternClassFilterTests extends TestCase {

	public void testInvalidPattern() {
		try {
			new TypePatternClassFilter("-");
			fail("Pattern must be recognized as invalid.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testValidPatternMatching() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("org.springframework.beans.*");
		assertTrue("Must match: in package", tpcf.matches(TestBean.class));
		assertTrue("Must match: in package", tpcf.matches(ITestBean.class));
		assertTrue("Must match: in package", tpcf.matches(IOther.class));
		assertFalse("Must be excluded: in wrong package", tpcf.matches(CountingTestBean.class));
		assertFalse("Must be excluded: in wrong package", tpcf.matches(BeanFactory.class));
		assertFalse("Must be excluded: in wrong package", tpcf.matches(DefaultListableBeanFactory.class));
	}

	public void testSubclassMatching() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("org.springframework.beans.ITestBean+");
		assertTrue("Must match: in package", tpcf.matches(TestBean.class));
		assertTrue("Must match: in package", tpcf.matches(ITestBean.class));
		assertTrue("Must match: in package", tpcf.matches(CountingTestBean.class));
		assertFalse("Must be excluded: not subclass", tpcf.matches(IOther.class));
		assertFalse("Must be excluded: not subclass", tpcf.matches(DefaultListableBeanFactory.class));
	}
	
	public void testAndOrNotReplacement() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("java.lang.Object or java.lang.String");
		assertFalse("matches Number",tpcf.matches(Number.class));
		assertTrue("matches Object",tpcf.matches(Object.class));
		assertTrue("matchesString",tpcf.matches(String.class));
		tpcf = new TypePatternClassFilter("java.lang.Number+ and java.lang.Float");
		assertTrue("matches Float",tpcf.matches(Float.class));
		assertFalse("matches Double",tpcf.matches(Double.class));
		tpcf = new TypePatternClassFilter("java.lang.Number+ and not java.lang.Float");
		assertFalse("matches Float",tpcf.matches(Float.class));
		assertTrue("matches Double",tpcf.matches(Double.class));	
	}

	public void testSetTypePatternWithNullArgument() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new TypePatternClassFilter(null);
			}
		}.runTest();
	}

	public void testInvocationOfMatchesMethodBlowsUpWhenNoTypePatternHasBeenSet() throws Exception {
		new AssertThrows(IllegalStateException.class) {
			public void test() throws Exception {
				new TypePatternClassFilter().matches(String.class);
			}
		}.runTest();
	}

}
