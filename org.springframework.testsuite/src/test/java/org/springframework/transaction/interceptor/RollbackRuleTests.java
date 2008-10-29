/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.springframework.beans.FatalBeanException;
import org.springframework.mail.MailSendException;

/**
 * Unit tests for the {@link RollbackRuleAttribute} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @since 09.04.2003
 */
public class RollbackRuleTests extends TestCase {

	public void testFoundImmediatelyWithString() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute("java.lang.Exception");
		assertTrue(rr.getDepth(new Exception()) == 0);
	}
	
	public void testFoundImmediatelyWithClass() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
		assertTrue(rr.getDepth(new Exception()) == 0);
	}
	
	public void testNotFound() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute("javax.servlet.ServletException");
		assertTrue(rr.getDepth(new MailSendException("")) == -1);
	}
	
	public void testAncestry() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute("java.lang.Exception");
		// Exception -> Runtime -> NestedRuntime -> MailException -> MailSendException
		assertTrue(rr.getDepth(new MailSendException("")) == 4);
	}

	public void testAlwaysTrueForThrowable() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute("java.lang.Throwable");
		assertTrue(rr.getDepth(new MailSendException("")) > 0);
		assertTrue(rr.getDepth(new ServletException()) > 0);
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
