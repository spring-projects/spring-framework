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
