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

package org.springframework.web.server.i18n;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static java.util.Locale.*;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AcceptHeaderLocaleContextResolver}.
 *
 * @author Sebastien Deleuze
 */
public class AcceptHeaderLocaleContextResolverTests {

	private AcceptHeaderLocaleContextResolver resolver = new AcceptHeaderLocaleContextResolver();


	@Test
	public void resolve() throws Exception {
		assertEquals(CANADA, this.resolver.resolveLocaleContext(exchange(CANADA)).getLocale());
		assertEquals(US, this.resolver.resolveLocaleContext(exchange(US, CANADA)).getLocale());
	}

	@Test
	public void resolvePreferredSupported() throws Exception {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertEquals(CANADA, this.resolver.resolveLocaleContext(exchange(US, CANADA)).getLocale());
	}

	@Test
	public void resolvePreferredNotSupported() throws Exception {
		this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
		assertEquals(US, this.resolver.resolveLocaleContext(exchange(US, UK)).getLocale());
	}

	@Test
	public void resolvePreferredNotSupportedWithDefault() {
		this.resolver.setSupportedLocales(Arrays.asList(US, JAPAN));
		this.resolver.setDefaultLocale(JAPAN);

		MockServerHttpRequest request = MockServerHttpRequest.get("/").acceptLanguageAsLocales(KOREA).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertEquals(JAPAN, this.resolver.resolveLocaleContext(exchange).getLocale());
	}

	@Test
	public void defaultLocale() throws Exception {
		this.resolver.setDefaultLocale(JAPANESE);
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		assertEquals(JAPANESE, this.resolver.resolveLocaleContext(exchange).getLocale());

		request = MockServerHttpRequest.get("/").acceptLanguageAsLocales(US).build();
		exchange = MockServerWebExchange.from(request);
		assertEquals(US, this.resolver.resolveLocaleContext(exchange).getLocale());
	}


	private ServerWebExchange exchange(Locale... locales) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("").acceptLanguageAsLocales(locales));
	}

}
