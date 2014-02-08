/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SerializationTestUtils;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Dmitriy Kopylenko
 * @author Chris Beams
 */
public abstract class AbstractRegexpMethodPointcutTests {

	private AbstractRegexpMethodPointcut rpc;

	@Before
	public void setUp() {
		rpc = getRegexpMethodPointcut();
	}

	protected abstract AbstractRegexpMethodPointcut getRegexpMethodPointcut();

	@Test
	public void testNoPatternSupplied() throws Exception {
		noPatternSuppliedTests(rpc);
	}

	@Test
	public void testSerializationWithNoPatternSupplied() throws Exception {
		rpc = (AbstractRegexpMethodPointcut) SerializationTestUtils.serializeAndDeserialize(rpc);
		noPatternSuppliedTests(rpc);
	}

	protected void noPatternSuppliedTests(AbstractRegexpMethodPointcut rpc) throws Exception {
		assertFalse(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), String.class));
		assertFalse(rpc.matches(Object.class.getMethod("wait", (Class[]) null), Object.class));
		assertEquals(0, rpc.getPatterns().length);
	}

	@Test
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

	@Test
	public void testSpecificMatch() throws Exception {
		rpc.setPattern("java.lang.String.hashCode");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), String.class));
		assertFalse(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), Object.class));
	}

	@Test
	public void testWildcard() throws Exception {
		rpc.setPattern(".*Object.hashCode");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), Object.class));
		assertFalse(rpc.matches(Object.class.getMethod("wait", (Class[]) null), Object.class));
	}

	@Test
	public void testWildcardForOneClass() throws Exception {
		rpc.setPattern("java.lang.Object.*");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", (Class[]) null), String.class));
		assertTrue(rpc.matches(Object.class.getMethod("wait", (Class[]) null), String.class));
	}

	@Test
	public void testMatchesObjectClass() throws Exception {
		rpc.setPattern("java.lang.Object.*");
		assertTrue(rpc.matches(Exception.class.getMethod("hashCode", (Class[]) null), IOException.class));
		// Doesn't match a method from Throwable
		assertFalse(rpc.matches(Exception.class.getMethod("getMessage", (Class[]) null), Exception.class));
	}

	@Test
	public void testWithExclusion() throws Exception {
		this.rpc.setPattern(".*get.*");
		this.rpc.setExcludedPattern(".*Age.*");
		assertTrue(this.rpc.matches(TestBean.class.getMethod("getName"), TestBean.class));
		assertFalse(this.rpc.matches(TestBean.class.getMethod("getAge"), TestBean.class));
	}

}
