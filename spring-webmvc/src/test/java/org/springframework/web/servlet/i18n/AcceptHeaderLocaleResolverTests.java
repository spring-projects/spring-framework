/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;

import static java.util.Locale.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AcceptHeaderLocaleResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class AcceptHeaderLocaleResolverTests {

	private AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();


	@Test
	public void resolve() throws Exception {
		assertEquals(CANADA, this.resolver.resolveLocale(request(CANADA)));
		assertEquals(US, this.resolver.resolveLocale(request(US, CANADA)));
	}

	@Test
	public void resolvePreferredSupported() throws Exception {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertEquals(CANADA, this.resolver.resolveLocale(request(US, CANADA)));
	}

	@Test
	public void resolvePreferredNotSupported() throws Exception {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertEquals(US, this.resolver.resolveLocale(request(US, UK)));
	}
	@Test

	public void resolvePreferredNotSupportedWithDefault() {
		this.resolver.setSupportedLocales(Arrays.asList(US, JAPAN));
		this.resolver.setDefaultLocale(Locale.JAPAN);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Language", KOREA.toLanguageTag());
		request.setPreferredLocales(Collections.singletonList(KOREA));
		assertEquals(Locale.JAPAN, this.resolver.resolveLocale(request));
	}

	@Test
	public void defaultLocale() throws Exception {
		this.resolver.setDefaultLocale(JAPANESE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		assertEquals(JAPANESE, this.resolver.resolveLocale(request));

		request.addHeader("Accept-Language", US.toLanguageTag());
		request.setPreferredLocales(Collections.singletonList(US));
		assertEquals(US, this.resolver.resolveLocale(request));
	}


	private HttpServletRequest request(Locale... locales) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setPreferredLocales(Arrays.asList(locales));
		return request;
	}

}
