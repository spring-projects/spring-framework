/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util;

import jakarta.servlet.http.MappingMatch;
import org.junit.jupiter.api.Test;

import org.springframework.http.server.RequestPath;
import org.springframework.web.testfixture.servlet.MockHttpServletMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ServletRequestPathUtils}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 */
class ServletRequestPathUtilsTests {

	@Test
	void parseAndCache() {
		// basic
		testParseAndCache("/app/servlet/a/b/c", "/app", "/servlet", "/a/b/c");

		// contextPath only, servletPathOnly, contextPath and servletPathOnly
		testParseAndCache("/app/a/b/c", "/app", "", "/a/b/c");
		testParseAndCache("/servlet/a/b/c", "", "/servlet", "/a/b/c");
		testParseAndCache("/app1/app2/servlet1/servlet2", "/app1/app2", "/servlet1/servlet2", "");

		// trailing slash
		testParseAndCache("/app/servlet/a/", "/app", "/servlet", "/a/");
		testParseAndCache("/app/servlet/a//", "/app", "/servlet", "/a//");
	}

	@Test
	void modifyPathContextWithExistingContextPath() {
		RequestPath requestPath = createRequestPath("/app/api/persons/42", "/app", "/api", "/persons/42");
		assertThatIllegalStateException().isThrownBy(() -> requestPath.modifyContextPath("/persons"))
				.withMessage("Could not change context path to '/api/persons': a context path is already specified");
	}

	@Test
	void modifyPathContextWhenContextPathIsNotInThePath() {
		RequestPath requestPath = createRequestPath("/api/persons/42", "", "/api", "/persons/42");
		assertThatIllegalArgumentException().isThrownBy(() -> requestPath.modifyContextPath("/something"))
				.withMessage("Invalid contextPath '/api/something': " +
						"must match the start of requestPath: '/api/persons/42'");
	}

	@Test
	void modifyPathContextReplacesServletPath() {
		RequestPath requestPath = createRequestPath("/api/persons/42", "", "/api", "/persons/42");
		RequestPath updatedRequestPath = requestPath.modifyContextPath("/persons");
		assertThat(updatedRequestPath.contextPath().value()).isEqualTo("/api/persons");
		assertThat(updatedRequestPath.pathWithinApplication().value()).isEqualTo("/42");
		assertThat(updatedRequestPath.value()).isEqualTo("/api/persons/42");
	}

	@Test
	void modifyPathContextWithContextPathNotStartingWithSlash() {
		RequestPath requestPath = createRequestPath("/api/persons/42", "", "/api", "/persons/42");
		assertThatIllegalArgumentException().isThrownBy(() -> requestPath.modifyContextPath("persons"))
				.withMessage("Invalid contextPath 'persons': must start with '/' and not end with '/'");
	}

	@Test
	void modifyPathContextWithContextPathEndingWithSlash() {
		RequestPath requestPath = createRequestPath("/api/persons/42", "", "/api", "/persons/42");
		assertThatIllegalArgumentException().isThrownBy(() -> requestPath.modifyContextPath("/persons/"))
				.withMessage("Invalid contextPath '/persons/': must start with '/' and not end with '/'");
	}

	private void testParseAndCache(
			String requestUri, String contextPath, String servletPath, String pathWithinApplication) {

		RequestPath requestPath = createRequestPath(requestUri, contextPath, servletPath, pathWithinApplication);

		assertThat(requestPath.contextPath().value()).isEqualTo(contextPath);
		assertThat(requestPath.pathWithinApplication().value()).isEqualTo(pathWithinApplication);
	}

	private static RequestPath createRequestPath(
			String requestUri, String contextPath, String servletPath, String pathWithinApplication) {

		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setContextPath(contextPath);
		request.setServletPath(servletPath);
		request.setHttpServletMapping(new MockHttpServletMapping(
				pathWithinApplication, contextPath, "myServlet", MappingMatch.PATH));

		return ServletRequestPathUtils.parseAndCache(request);
	}

}
