/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HeaderApiVersionResolver}.
 *
 * @author Rossen Stoyanchev
 */
class HeaderApiVersionResolverTests {

	private final String headerName = "Api-Version";

	private final HeaderApiVersionResolver resolver = new HeaderApiVersionResolver(headerName);


	@Test
	void resolve() {
		String version = "1.2";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
		request.addHeader(headerName, version);
		String actual = resolver.resolveVersion(request);
		assertThat(actual).isEqualTo(version);
	}

	@Test
	void noHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
		String version = resolver.resolveVersion(request);
		assertThat(version).isNull();
	}

}
