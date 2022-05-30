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

package org.springframework.web.server.i18n;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

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
 * Unit tests for {@link AcceptHeaderLocaleContextResolver}.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 */
public class AcceptHeaderLocaleContextResolverTests {

	private final AcceptHeaderLocaleContextResolver resolver = new AcceptHeaderLocaleContextResolver();


	@Test
	public void resolve() {
		assertThat(this.resolver.resolveLocaleContext(exchange(CANADA)).getLocale()).isEqualTo(CANADA);
		assertThat(this.resolver.resolveLocaleContext(exchange(US, CANADA)).getLocale()).isEqualTo(US);
	}

	@Test
	public void resolvePreferredSupported() {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertThat(this.resolver.resolveLocaleContext(exchange(US, CANADA)).getLocale()).isEqualTo(CANADA);
	}

	@Test
	public void resolvePreferredNotSupported() {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertThat(this.resolver.resolveLocaleContext(exchange(US, UK)).getLocale()).isEqualTo(US);
	}

	@Test
	public void resolvePreferredNotSupportedWithDefault() {
		this.resolver.setSupportedLocales(Arrays.asList(US, JAPAN));
		this.resolver.setDefaultLocale(JAPAN);
		assertThat(this.resolver.resolveLocaleContext(exchange(KOREA)).getLocale()).isEqualTo(JAPAN);
	}

	@Test
	public void resolvePreferredAgainstLanguageOnly() {
		this.resolver.setSupportedLocales(Collections.singletonList(ENGLISH));
		assertThat(this.resolver.resolveLocaleContext(exchange(GERMANY, US, UK)).getLocale()).isEqualTo(ENGLISH);
	}

	@Test
	public void resolvePreferredAgainstCountryIfPossible() {
		this.resolver.setSupportedLocales(Arrays.asList(ENGLISH, UK));
		assertThat(this.resolver.resolveLocaleContext(exchange(GERMANY, US, UK)).getLocale()).isEqualTo(UK);
	}

	@Test
	public void resolvePreferredAgainstLanguageWithMultipleSupportedLocales() {
		this.resolver.setSupportedLocales(Arrays.asList(GERMAN, US));
		assertThat(this.resolver.resolveLocaleContext(exchange(GERMANY, US, UK)).getLocale()).isEqualTo(GERMAN);
	}

	@Test
	public void resolveMissingAcceptLanguageHeader() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isNull();
	}

	@Test
	public void resolveMissingAcceptLanguageHeaderWithDefault() {
		this.resolver.setDefaultLocale(US);

		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isEqualTo(US);
	}

	@Test
	public void resolveEmptyAcceptLanguageHeader() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(HttpHeaders.ACCEPT_LANGUAGE, "").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isNull();
	}

	@Test
	public void resolveEmptyAcceptLanguageHeaderWithDefault() {
		this.resolver.setDefaultLocale(US);

		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(HttpHeaders.ACCEPT_LANGUAGE, "").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isEqualTo(US);
	}

	@Test
	public void resolveInvalidAcceptLanguageHeader() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(HttpHeaders.ACCEPT_LANGUAGE, "en_US").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isNull();
	}

	@Test
	public void resolveInvalidAcceptLanguageHeaderWithDefault() {
		this.resolver.setDefaultLocale(US);

		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(HttpHeaders.ACCEPT_LANGUAGE, "en_US").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isEqualTo(US);
	}

	@Test
	public void defaultLocale() {
		this.resolver.setDefaultLocale(JAPANESE);
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isEqualTo(JAPANESE);

		request = MockServerHttpRequest.get("/").acceptLanguageAsLocales(US).build();
		exchange = MockServerWebExchange.from(request);
		assertThat(this.resolver.resolveLocaleContext(exchange).getLocale()).isEqualTo(US);
	}


	private ServerWebExchange exchange(Locale... locales) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("").acceptLanguageAsLocales(locales));
	}

}
