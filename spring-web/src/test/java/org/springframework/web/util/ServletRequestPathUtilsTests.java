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

package org.springframework.web.util;

import javax.servlet.http.MappingMatch;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.RequestPath;
import org.springframework.web.testfixture.servlet.MockHttpServletMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServletRequestPathUtils}.
 * @author Rossen Stoyanchev
 */
public class ServletRequestPathUtilsTests {

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

	private void testParseAndCache(
			String requestUri, String contextPath, String servletPath, String pathWithinApplication) {

		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setContextPath(contextPath);
		request.setServletPath(servletPath);
		request.setHttpServletMapping(new MockHttpServletMapping(
				pathWithinApplication, contextPath, "myServlet", MappingMatch.PATH));

		RequestPath requestPath = ServletRequestPathUtils.parseAndCache(request);

		assertThat(requestPath.contextPath().value()).isEqualTo(contextPath);
		assertThat(requestPath.pathWithinApplication().value()).isEqualTo(pathWithinApplication);
	}

}
