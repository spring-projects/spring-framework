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

import org.springframework.http.server.PathContainer;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link org.springframework.web.accept.PathApiVersionResolver}.
 * @author Rossen Stoyanchev
 * @author Martin Mois
 */
public class PathApiVersionResolverTests {

	@Test
	void resolve() {
		testResolve(0, "/1.0/path", "1.0");
		testResolve(1, "/app/1.1/path", "1.1");
	}

	@Test
	void insufficientPathSegments() {
		assertThatThrownBy(() -> testResolve(0, "/", "1.0")).isInstanceOf(InvalidApiVersionException.class);
	}

	@Test
	void excludePathTrue() {
		String requestUri = "/v3/api-docs";
		testResolveWithExcludePath(requestUri, null);
	}

	@Test
	void excludePathFalse() {
		String requestUri = "/app/1.0/path";
		testResolveWithExcludePath(requestUri, "1.0");
	}

	@Test
	void excludePathFalseShortPath() {
		String requestUri = "/app";
		assertThatThrownBy(() -> testResolveWithExcludePath(requestUri, null)).isInstanceOf(InvalidApiVersionException.class);
	}

	private static void testResolveWithExcludePath(String requestUri, String expected) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(requestUri));
		String actual = new PathApiVersionResolver(1, requestPath -> {
			List<PathContainer.Element> elements = requestPath.elements();
			if (elements.size() < 4) {
				return false;
			}
			return elements.get(0).value().equals("/") &&
					elements.get(1).value().equals("v3") &&
					elements.get(2).value().equals("/") &&
					elements.get(3).value().equals("api-docs");
		}).resolveVersion(exchange);
		assertThat(actual).isEqualTo(expected);
	}

	private static void testResolve(int index, String requestUri, String expected) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(requestUri));
		String actual = new PathApiVersionResolver(index).resolveVersion(exchange);
		assertThat(actual).isEqualTo(expected);
	}

}
