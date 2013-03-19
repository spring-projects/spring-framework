/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.context.request;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpSession;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ServletRequestAttributesTests {

	private static final String KEY = "ThatThingThatThing";

	@SuppressWarnings("serial")
	private static final Serializable VALUE = new Serializable() {
	};

	@Test(expected = IllegalArgumentException.class)
	public void ctorRejectsNullArg() throws Exception {
		new ServletRequestAttributes(null);
	}

	@Test
	public void updateAccessedAttributes() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		Object value = attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		assertSame(VALUE, value);
		attrs.requestCompleted();
	}

	@Test
	public void setRequestScopedAttribute() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_REQUEST);
		Object value = request.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	@Test
	public void setRequestScopedAttributeAfterCompletion() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		request.close();
		try {
			attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_REQUEST);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void setSessionScopedAttribute() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	@Test
	public void setSessionScopedAttributeAfterCompletion() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.requestCompleted();
		request.close();
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	@Test
	public void setGlobalSessionScopedAttribute() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_GLOBAL_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	@Test
	public void setGlobalSessionScopedAttributeAfterCompletion() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.requestCompleted();
		request.close();
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_GLOBAL_SESSION);
		Object value = session.getAttribute(KEY);
		assertSame(VALUE, value);
	}

	@Test
	public void getSessionScopedAttributeDoesNotForceCreationOfSession() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);

		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		Object value = attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		assertNull(value);
		verify(request).getSession(false);
	}

	@Test
	public void removeSessionScopedAttribute() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.removeAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		Object value = session.getAttribute(KEY);
		assertNull(value);
	}

	@Test
	public void removeSessionScopedAttributeDoesNotForceCreationOfSession() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);

		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.removeAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		verify(request).getSession(false);
	}

}
