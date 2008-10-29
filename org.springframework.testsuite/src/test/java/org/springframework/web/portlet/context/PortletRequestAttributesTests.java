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

package org.springframework.web.portlet.context;

import java.io.Serializable;

import javax.portlet.PortletRequest;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.mock.web.portlet.MockPortletSession;
import org.springframework.test.AssertThrows;
import org.springframework.web.context.request.RequestAttributes;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class PortletRequestAttributesTests extends TestCase {

	private static final String KEY = "ThatThingThatThing";


	private static final Serializable VALUE = new Serializable() {
	};


	public void testCtorRejectsNullArg() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new PortletRequestAttributes(null);
			}
		}.runTest();
	}

	public void testUpdateAccessedAttributes() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute(KEY, VALUE);
		MockPortletRequest request = new MockPortletRequest();
		request.setSession(session);
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		Object value = attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		assertSame(VALUE, value);
		attrs.requestCompleted();
	}

	public void testSetRequestScopedAttribute() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_REQUEST);
		Object value = request.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	public void testSetRequestScopedAttributeAfterCompletion() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		request.close();
		try {
			attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_REQUEST);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	public void testSetSessionScopedAttribute() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute(KEY, VALUE);
		MockPortletRequest request = new MockPortletRequest();
		request.setSession(session);
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	public void testSetSessionScopedAttributeAfterCompletion() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute(KEY, VALUE);
		MockPortletRequest request = new MockPortletRequest();
		request.setSession(session);
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		attrs.requestCompleted();
		request.close();
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	public void testSetGlobalSessionScopedAttribute() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute(KEY, VALUE);
		MockPortletRequest request = new MockPortletRequest();
		request.setSession(session);
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_GLOBAL_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	public void testSetGlobalSessionScopedAttributeAfterCompletion() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute(KEY, VALUE);
		MockPortletRequest request = new MockPortletRequest();
		request.setSession(session);
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		attrs.requestCompleted();
		request.close();
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_GLOBAL_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	public void testGetSessionScopedAttributeDoesNotForceCreationOfSession() throws Exception {
		MockControl mockRequest = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mockRequest.getMock();
		request.getPortletSession(false);
		mockRequest.setReturnValue(null, 1);
		mockRequest.replay();

		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		Object value = attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		assertNull(value);

		mockRequest.verify();
	}

	public void testRemoveSessionScopedAttribute() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute(KEY, VALUE);
		MockPortletRequest request = new MockPortletRequest();
		request.setSession(session);
		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		attrs.removeAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		Object value = session.getAttribute(KEY);
		assertNull(value);
	}

	public void testRemoveSessionScopedAttributeDoesNotForceCreationOfSession() throws Exception {
		MockControl mockRequest = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mockRequest.getMock();
		request.getPortletSession(false);
		mockRequest.setReturnValue(null, 1);
		mockRequest.replay();

		PortletRequestAttributes attrs = new PortletRequestAttributes(request);
		attrs.removeAttribute(KEY, RequestAttributes.SCOPE_SESSION);

		mockRequest.verify();
	}

}
