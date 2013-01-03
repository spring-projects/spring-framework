/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.interceptor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import junit.framework.TestCase;

import org.springframework.beans.FatalBeanException;

/**
 * Unit tests for the {@link RollbackRuleAttribute} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @since 09.04.2003
 */
public class RollbackRuleTests extends TestCase {

	public void testFoundImmediatelyWithString() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.lang.Exception.class.getName());
		assertTrue(rr.getDepth(new Exception()) == 0);
	}

	public void testFoundImmediatelyWithClass() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
		assertTrue(rr.getDepth(new Exception()) == 0);
	}

	public void testNotFound() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.io.IOException.class.getName());
		assertTrue(rr.getDepth(new MyRuntimeException("")) == -1);
	}

	public void testAncestry() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.lang.Exception.class.getName());
		// Exception -> Runtime -> NestedRuntime -> MyRuntimeException
		assertThat(rr.getDepth(new MyRuntimeException("")), equalTo(3));
	}

	public void testAlwaysTrueForThrowable() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.lang.Throwable.class.getName());
		assertTrue(rr.getDepth(new MyRuntimeException("")) > 0);
		assertTrue(rr.getDepth(new IOException()) > 0);
		assertTrue(rr.getDepth(new FatalBeanException(null,null)) > 0);
		assertTrue(rr.getDepth(new RuntimeException()) > 0);
	}

	public void testCtorArgMustBeAThrowableClassWithNonThrowableType() {
		try {
			new RollbackRuleAttribute(StringBuffer.class);
			fail("Cannot construct a RollbackRuleAttribute with a non-Throwable type");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorArgMustBeAThrowableClassWithNullThrowableType() {
		try {
			new RollbackRuleAttribute((Class) null);
			fail("Cannot construct a RollbackRuleAttribute with a null-Throwable type");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorArgExceptionStringNameVersionWithNull() {
		try {
			new RollbackRuleAttribute((String) null);
			fail("Cannot construct a RollbackRuleAttribute with a null-Throwable type");
		}
		catch (IllegalArgumentException expected) {
		}
	}

}
