/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultRequestPath}.
 * @author Rossen Stoyanchev
 */
class DefaultRequestPathTests {

	@Test
	void parse() {
		// basic
		testParse("/app/a/b/c", "/app", "/a/b/c");

		// no context path
		testParse("/a/b/c", "", "/a/b/c");

		// context path only
		testParse("/a/b", "/a/b", "");

		// root path
		testParse("/", "", "/");

		// empty path
		testParse("", "", "");
		testParse("", "/", "");

		// trailing slash
		testParse("/app/a/", "/app", "/a/");
		testParse("/app/a//", "/app", "/a//");
	}

	private void testParse(String fullPath, String contextPath, String pathWithinApplication) {
		RequestPath requestPath = RequestPath.parse(fullPath, contextPath);
		Object expected = contextPath.equals("/") ? "" : contextPath;
		assertThat(requestPath.contextPath().value()).isEqualTo(expected);
		assertThat(requestPath.pathWithinApplication().value()).isEqualTo(pathWithinApplication);
	}

	@Test
	void modifyContextPath() {
		RequestPath requestPath = RequestPath.parse("/aA/bB/cC", null);

		assertThat(requestPath.contextPath().value()).isEqualTo("");
		assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/aA/bB/cC");

		requestPath = requestPath.modifyContextPath("/aA");

		assertThat(requestPath.contextPath().value()).isEqualTo("/aA");
		assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/bB/cC");
	}

}
