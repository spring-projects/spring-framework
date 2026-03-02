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

package org.springframework.web.accept;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.PathContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PathApiVersionResolver}.
 * @author Rossen Stoyanchev
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
	void includePathFalse() {
		String requestUri = "/v3/api-docs";
		testResolveWithIncludePath(requestUri, null);
	}

	@Test
	void includePathTrue() {
		String requestUri = "/app/1.0/path";
		testResolveWithIncludePath(requestUri, "1.0");
	}

	@Test
	void includePathFalseShortPath() {
		String requestUri = "/app";
		testResolveWithIncludePath(requestUri, null);
	}

	@Test
	void includePathInsufficientPathSegments() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app");
		try {
			ServletRequestPathUtils.parseAndCache(request);
			assertThatThrownBy(() -> new PathApiVersionResolver(1, requestPath -> true)
					.resolveVersion(request))
				.isInstanceOf(InvalidApiVersionException.class);
		}
		finally {
			ServletRequestPathUtils.clearParsedRequestPath(request);
		}
	}

	private static void testResolveWithIncludePath(String requestUri, String expected) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		try {
			ServletRequestPathUtils.parseAndCache(request);
			String actual = new PathApiVersionResolver(1, requestPath -> {
				List<PathContainer.Element> elements = requestPath.elements();
				if (elements.size() < 4) {
					return false;
				}
				return elements.get(0).value().equals("/") &&
						elements.get(1).value().equals("app") &&
						elements.get(2).value().equals("/") &&
						elements.get(3).value().equals("1.0");
			}).resolveVersion(request);
			assertThat(actual).isEqualTo(expected);
		}
		finally {
			ServletRequestPathUtils.clearParsedRequestPath(request);
		}
	}

	private static void testResolve(int index, String requestUri, String expected) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		try {
			ServletRequestPathUtils.parseAndCache(request);
			String actual = new PathApiVersionResolver(index).resolveVersion(request);
			assertThat(actual).isEqualTo(expected);
		}
		finally {
			ServletRequestPathUtils.clearParsedRequestPath(request);
		}
	}

}
