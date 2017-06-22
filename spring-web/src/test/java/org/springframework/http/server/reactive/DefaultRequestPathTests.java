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
package org.springframework.http.server.reactive;

import java.net.URI;

import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultRequestPath}.
 * @author Rossen Stoyanchev
 */
public class DefaultRequestPathTests {

	@Test
	public void requestPath() throws Exception {
		// basic
		testRequestPath("/app/a/b/c", "/app", "/a/b/c", false, true, false);

		// no context path
		testRequestPath("/a/b/c", "", "/a/b/c", false, true, false);

		// context path only
		testRequestPath("/a/b", "/a/b", "", false, true, false);

		// root path
		testRequestPath("/", "", "/", false, true, false);

		// empty path
		testRequestPath("", "", "", true, false, false);
		testRequestPath("", "/", "", true, false, false);

		// trailing slash
		testRequestPath("/app/a/", "/app", "/a/", false, true, true);
		testRequestPath("/app/a//", "/app", "/a//", false, true, true);
	}

	private void testRequestPath(String fullPath, String contextPath, String pathWithinApplication,
			boolean empty, boolean absolute, boolean trailingSlash) {

		URI uri = URI.create("http://localhost:8080" + fullPath);
		RequestPath requestPath = new DefaultRequestPath(uri, contextPath, UTF_8);

		assertEquals(empty, requestPath.isEmpty());
		assertEquals(absolute, requestPath.isAbsolute());
		assertEquals(trailingSlash, requestPath.hasTrailingSlash());
		assertEquals(contextPath.equals("/") ? "" : contextPath, requestPath.contextPath().value());
		assertEquals(pathWithinApplication, requestPath.pathWithinApplication().value());
	}

}
