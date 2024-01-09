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

package org.springframework.web.context.request;

import java.io.Serializable;
import java.math.BigInteger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
class ServletRequestAttributesTests {

	private static final String KEY = "ThatThingThatThing";

	@SuppressWarnings("serial")
	private static final Serializable VALUE = new Serializable() {
	};


	@Test
	void ctorRejectsNullArg() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ServletRequestAttributes(null));
	}

	@Test
	void setRequestScopedAttribute() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_REQUEST);
		Object value = request.getAttribute(KEY);
		assertThat(value).isSameAs(VALUE);
	}

	@Test
	void setRequestScopedAttributeAfterCompletion() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		request.close();
		assertThatIllegalStateException().isThrownBy(() ->
				attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_REQUEST));
	}

	@Test
	void setSessionScopedAttribute() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_SESSION);
		assertThat(session.getAttribute(KEY)).isSameAs(VALUE);
	}

	@Test
	void setSessionScopedAttributeAfterCompletion() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		assertThat(attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION)).isSameAs(VALUE);
		attrs.requestCompleted();
		request.close();
		attrs.setAttribute(KEY, VALUE, RequestAttributes.SCOPE_SESSION);
		assertThat(session.getAttribute(KEY)).isSameAs(VALUE);
	}

	@Test
	void getSessionScopedAttributeDoesNotForceCreationOfSession() {
		HttpServletRequest request = mock();

		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		Object value = attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		assertThat(value).isNull();
		verify(request).getSession(false);
	}

	@Test
	void removeSessionScopedAttribute() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(KEY, VALUE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.removeAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		Object value = session.getAttribute(KEY);
		assertThat(value).isNull();
	}

	@Test
	void removeSessionScopedAttributeDoesNotForceCreationOfSession() {
		HttpServletRequest request = mock();

		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.removeAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		verify(request).getSession(false);
	}

	@Test
	void updateAccessedAttributes() {
		HttpServletRequest request = mock();
		HttpSession session = mock();
		given(request.getSession(anyBoolean())).willReturn(session);
		given(session.getAttribute(KEY)).willReturn(VALUE);

		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		assertThat(attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION)).isSameAs(VALUE);
		attrs.requestCompleted();

		verify(session, times(2)).getAttribute(KEY);
		verify(session).setAttribute(KEY, VALUE);
		verifyNoMoreInteractions(session);
	}

	@Test
	void skipImmutableString() {
		doSkipImmutableValue("someString");
	}

	@Test
	void skipImmutableCharacter() {
		doSkipImmutableValue('x');
	}

	@Test
	void skipImmutableBoolean() {
		doSkipImmutableValue(Boolean.TRUE);
	}

	@Test
	void skipImmutableInteger() {
		doSkipImmutableValue(1);
	}

	@Test
	void skipImmutableFloat() {
		doSkipImmutableValue(1.1F);
	}

	@Test
	void skipImmutableBigInteger() {
		doSkipImmutableValue(new BigInteger("1"));
	}

	private void doSkipImmutableValue(Object immutableValue) {
		HttpServletRequest request = mock();
		HttpSession session = mock();
		given(request.getSession(anyBoolean())).willReturn(session);
		given(session.getAttribute(KEY)).willReturn(immutableValue);

		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		attrs.getAttribute(KEY, RequestAttributes.SCOPE_SESSION);
		attrs.requestCompleted();

		verify(session, times(2)).getAttribute(KEY);
		verifyNoMoreInteractions(session);
	}

}
