/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;

import static java.util.Locale.CANADA;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static java.util.Locale.GERMANY;
import static java.util.Locale.JAPAN;
import static java.util.Locale.JAPANESE;
import static java.util.Locale.KOREA;
import static java.util.Locale.UK;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AcceptHeaderLocaleResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class AcceptHeaderLocaleResolverTests {

	private final AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();


	@Test
	public void resolve() {
		assertThat(this.resolver.resolveLocale(request(CANADA))).isEqualTo(CANADA);
		assertThat(this.resolver.resolveLocale(request(US, CANADA))).isEqualTo(US);
	}

	@Test
	public void resolvePreferredSupported() {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertThat(this.resolver.resolveLocale(request(US, CANADA))).isEqualTo(CANADA);
	}

	@Test
	public void resolvePreferredNotSupported() {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertThat(this.resolver.resolveLocale(request(US, UK))).isEqualTo(US);
	}

	@Test
	public void resolvePreferredAgainstLanguageOnly() {
		this.resolver.setSupportedLocales(Collections.singletonList(ENGLISH));
		assertThat(this.resolver.resolveLocale(request(GERMANY, US, UK))).isEqualTo(ENGLISH);
	}

	@Test
	public void resolvePreferredAgainstCountryIfPossible() {
		this.resolver.setSupportedLocales(Arrays.asList(ENGLISH, UK));
		assertThat(this.resolver.resolveLocale(request(GERMANY, US, UK))).isEqualTo(UK);
	}

	@Test
	public void resolvePreferredAgainstLanguageWithMultipleSupportedLocales() {
		this.resolver.setSupportedLocales(Arrays.asList(GERMAN, US));
		assertThat(this.resolver.resolveLocale(request(GERMANY, US, UK))).isEqualTo(GERMAN);
	}

	@Test
	public void resolvePreferredNotSupportedWithDefault() {
		this.resolver.setSupportedLocales(Arrays.asList(US, JAPAN));
		this.resolver.setDefaultLocale(Locale.JAPAN);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Language", KOREA.toLanguageTag());
		request.setPreferredLocales(Collections.singletonList(KOREA));
		assertThat(this.resolver.resolveLocale(request)).isEqualTo(Locale.JAPAN);
	}

	@Test
	public void defaultLocale() {
		this.resolver.setDefaultLocale(JAPANESE);
		MockHttpServletRequest request = new MockHttpServletRequest();
		assertThat(this.resolver.resolveLocale(request)).isEqualTo(JAPANESE);

		request.addHeader("Accept-Language", US.toLanguageTag());
		request.setPreferredLocales(Collections.singletonList(US));
		assertThat(this.resolver.resolveLocale(request)).isEqualTo(US);
	}


	private HttpServletRequest request(Locale... locales) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setPreferredLocales(Arrays.asList(locales));
		return request;
	}

}
