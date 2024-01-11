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

package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SessionLocaleResolver}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Vedran Pavic
 */
class SessionLocaleResolverTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private SessionLocaleResolver resolver = new SessionLocaleResolver();


	@Test
	void resolveLocale() {
		request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.GERMAN);
	}

	@Test
	void setAndResolveLocale() {
		resolver.setLocale(request, response, Locale.GERMAN);
		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.GERMAN);

		HttpSession session = request.getSession();
		request = new MockHttpServletRequest();
		request.setSession(session);
		resolver = new SessionLocaleResolver();

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.GERMAN);
	}

	@Test
	void resolveLocaleWithoutSession() {
		request.addPreferredLocale(Locale.TAIWAN);

		assertThat(resolver.resolveLocale(request)).isEqualTo(request.getLocale());
	}

	@Test
	void resolveLocaleWithoutSessionAndDefaultLocale() {
		request.addPreferredLocale(Locale.TAIWAN);

		resolver.setDefaultLocale(Locale.GERMAN);

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.GERMAN);
	}

	@Test
	void setLocaleToNullLocale() {
		request.addPreferredLocale(Locale.TAIWAN);
		request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getSession().getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
		assertThat(locale).isNull();

		HttpSession session = request.getSession();
		request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		request.setSession(session);
		resolver = new SessionLocaleResolver();
		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.TAIWAN);
	}

	@Test
	void customDefaultLocaleFunction() {
		request.addPreferredLocale(Locale.TAIWAN);

		resolver.setDefaultLocaleFunction(request -> Locale.GERMAN);

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.GERMAN);
	}

	@Test
	void customDefaultTimeZoneFunction() {
		request.addPreferredLocale(Locale.TAIWAN);

		resolver.setDefaultTimeZoneFunction(request -> TimeZone.getTimeZone("GMT+1"));

		TimeZoneAwareLocaleContext context = (TimeZoneAwareLocaleContext) resolver.resolveLocaleContext(request);
		assertThat(context.getLocale()).isEqualTo(Locale.TAIWAN);
		assertThat(context.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

}
