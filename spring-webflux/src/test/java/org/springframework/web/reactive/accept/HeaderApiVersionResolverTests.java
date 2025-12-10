/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.accept;

import org.junit.jupiter.api.Test;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Unit tests for {@link HeaderApiVersionResolver}.
 *
 * @author Rossen Stoyanchev
 */
class HeaderApiVersionResolverTests {

	private final String headerName = "Api-Version";

	private final HeaderApiVersionResolver resolver = new HeaderApiVersionResolver(headerName);


	@Test
	void resolve() {
		String version = "1.2";
		ServerWebExchange exchange = MockServerWebExchange.from(get("/").header(headerName, version));
		String actual = resolver.resolveVersion(exchange);
		assertThat(actual).isEqualTo(version);
	}

	@Test
	void noHeader() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));
		String version = resolver.resolveVersion(exchange);
		assertThat(version).isNull();
	}

}
