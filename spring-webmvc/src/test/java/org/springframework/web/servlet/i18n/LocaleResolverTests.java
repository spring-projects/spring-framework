/*
 * Copyright 2002-2005 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.servlet.LocaleResolver;

/**
 * @author Juergen Hoeller
 * @since 20.03.2003
 */
public class LocaleResolverTests extends TestCase {

	private void internalTest(LocaleResolver localeResolver, boolean shouldSet) {
		// create mocks
		MockServletContext context = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(context);
		request.addPreferredLocale(Locale.UK);
		MockHttpServletResponse response = new MockHttpServletResponse();
		// check original locale
		Locale locale = localeResolver.resolveLocale(request);
		assertEquals(locale, Locale.UK);
		// set new locale
		try {
			localeResolver.setLocale(request, response, Locale.GERMANY);
			if (!shouldSet)
				fail("should not be able to set Locale");
			// check new locale
			locale = localeResolver.resolveLocale(request);
			assertEquals(locale, Locale.GERMANY);
		}
		catch (UnsupportedOperationException ex) {
			if (shouldSet)
				fail("should be able to set Locale");
		}
	}

	public void testAcceptHeaderLocaleResolver() {
		internalTest(new AcceptHeaderLocaleResolver(), false);
	}

	public void testCookieLocaleResolver() {
		internalTest(new CookieLocaleResolver(), true);
	}

	public void testSessionLocaleResolver() {
		internalTest(new SessionLocaleResolver(), true);
	}

}
