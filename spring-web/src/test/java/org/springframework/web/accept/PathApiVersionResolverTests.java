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

import org.junit.jupiter.api.Test;

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
