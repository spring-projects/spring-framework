/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class QueryApiVersionResolverTests {

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
