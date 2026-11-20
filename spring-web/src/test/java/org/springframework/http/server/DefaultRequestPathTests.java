/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultRequestPath}.
 *
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

		assertThat(requestPath.contextPath().value()).isEmpty();
		assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/aA/bB/cC");

		requestPath = requestPath.modifyContextPath("/aA");

		assertThat(requestPath.contextPath().value()).isEqualTo("/aA");
		assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/bB/cC");
	}

}
