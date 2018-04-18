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

package org.springframework.web.servlet.i18n;

import java.util.Locale;
import javax.servlet.http.HttpSession;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class SessionLocaleResolverTests {

	@Test
	public void testResolveLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

		SessionLocaleResolver resolver = new SessionLocaleResolver();
		assertEquals(Locale.GERMAN, resolver.resolveLocale(request));
	}

	@Test
	public void testSetAndResolveLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		SessionLocaleResolver resolver = new SessionLocaleResolver();
		resolver.setLocale(request, response, Locale.GERMAN);
		assertEquals(Locale.GERMAN, resolver.resolveLocale(request));

		HttpSession session = request.getSession();
		request = new MockHttpServletRequest();
		request.setSession(session);
		resolver = new SessionLocaleResolver();

		assertEquals(Locale.GERMAN, resolver.resolveLocale(request));
	}

	@Test
	public void testResolveLocaleWithoutSession() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		SessionLocaleResolver resolver = new SessionLocaleResolver();

		assertEquals(request.getLocale(), resolver.resolveLocale(request));
	}

	@Test
	public void testResolveLocaleWithoutSessionAndDefaultLocale() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		SessionLocaleResolver resolver = new SessionLocaleResolver();
		resolver.setDefaultLocale(Locale.GERMAN);

		assertEquals(Locale.GERMAN, resolver.resolveLocale(request));
	}

	@Test
	public void testSetLocaleToNullLocale() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);
		MockHttpServletResponse response = new MockHttpServletResponse();

		SessionLocaleResolver resolver = new SessionLocaleResolver();
		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getSession().getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
		assertNull(locale);

		HttpSession session = request.getSession();
		request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		request.setSession(session);
		resolver = new SessionLocaleResolver();
		assertEquals(Locale.TAIWAN, resolver.resolveLocale(request));
	}

}
