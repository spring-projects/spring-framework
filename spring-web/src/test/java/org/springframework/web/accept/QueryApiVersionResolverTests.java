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


import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link QueryApiVersionResolver}.
 * @author Rossen Stoyanchev
 */
public class QueryApiVersionResolverTests {

	private final String queryParamName = "api-version";

	private final QueryApiVersionResolver resolver = new QueryApiVersionResolver(queryParamName);


	@Test
	void resolve() {
		MockHttpServletRequest request = initRequest("q=foo&" + queryParamName + "=1.2");
		String version = resolver.resolveVersion(request);
		assertThat(version).isEqualTo("1.2");
	}

	@Test
	void noQueryString() {
		MockHttpServletRequest request = initRequest(null);
		String version = resolver.resolveVersion(request);
		assertThat(version).isNull();
	}

	@Test
	void noQueryParam() {
		MockHttpServletRequest request = initRequest("q=foo");
		String version = resolver.resolveVersion(request);
		assertThat(version).isNull();
	}

	private static MockHttpServletRequest initRequest(@Nullable String queryString) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
		request.setQueryString(queryString);
		return request;
	}

}
