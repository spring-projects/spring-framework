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

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.util.SerializationTestUtils;

/**
 * @author Rod Johnson
 * @author Dmitriy Kopylenko
 */
public abstract class AbstractRegexpMethodPointcutTests extends TestCase {

	private AbstractRegexpMethodPointcut rpc;

	protected void setUp() {
		rpc = getRegexpMethodPointcut();
	}

	protected abstract AbstractRegexpMethodPointcut getRegexpMethodPointcut();

	public void testNoPatternSupplied() throws Exception {
		noPatternSuppliedTests(rpc);
	}

	public void testSerializationWithNoPatternSupplied() throws Exception {
		rpc = (AbstractRegexpMethodPointcut) SerializationTestUtils.serializeAndDeserialize(rpc);
		noPatternSuppliedTests(rpc);
	}

	protected void noPatternSuppliedTests(AbstractRegexpMethodPointcut rpc) throws Exception {
		assertFalse(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), String.class));
		assertFalse(rpc.matches(Object.class.getMethod("wait", (Class[]) null), Object.class));
		assertEquals(0, rpc.getPatterns().length);
	}

	public void testExactMatch() throws Exception {
		rpc.setPattern("java.lang.Object.hashCode");
		exactMatchTests(rpc);
		rpc = (AbstractRegexpMethodPointcut) SerializationTestUtils.serializeAndDeserialize(rpc);
		exactMatchTests(rpc);
	}

	protected void exactMatchTests(AbstractRegexpMethodPointcut rpc) throws Exception {
		// assumes rpc.setPattern("java.lang.Object.hashCode");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), String.class));
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), Object.class));
		assertFalse(rpc.matches(Object.class.getMethod("wait", (Class[]) null), Object.class));
	}

	public void testSpecificMatch() throws Exception {
		rpc.setPattern("java.lang.String.hashCode");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), String.class));
		assertFalse(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), Object.class));
	}

	public void testWildcard() throws Exception {
		rpc.setPattern(".*Object.hashCode");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), Object.class));
		assertFalse(rpc.matches(Object.class.getMethod("wait", (Class[]) null), Object.class));
	}

	public void testWildcardForOneClass() throws Exception {
		rpc.setPattern("java.lang.Object.*");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), String.class));
		assertTrue(rpc.matches(Object.class.getMethod("wait", (Class[]) null), String.class));
	}

	public void testMatchesObjectClass() throws Exception {
		rpc.setPattern("java.lang.Object.*");
		assertTrue(rpc.matches(Exception.class.getMethod("hashCode", (Class[]) null), ServletException.class));
		// Doesn't match a method from Throwable
		assertFalse(rpc.matches(Exception.class.getMethod("getMessage", (Class[]) null), Exception.class));
	}

	public void testWithExclusion() throws Exception {
		this.rpc.setPattern(".*get.*");
		this.rpc.setExcludedPattern(".*Age.*");
		assertTrue(this.rpc.matches(TestBean.class.getMethod("getName", null), TestBean.class));
		assertFalse(this.rpc.matches(TestBean.class.getMethod("getAge", null), TestBean.class));
	}

}
