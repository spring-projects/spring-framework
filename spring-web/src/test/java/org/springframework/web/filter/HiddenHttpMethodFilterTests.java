/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HiddenHttpMethodFilter}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
class HiddenHttpMethodFilterTests {

	private final HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();

	@Test
	void filterWithParameter() throws IOException, ServletException {
		filterWithParameterForMethod("delete", "DELETE");
		filterWithParameterForMethod("put", "PUT");
		filterWithParameterForMethod("patch", "PATCH");
	}

	@Test
	void filterWithParameterDisallowedMethods() throws IOException, ServletException {
		filterWithParameterForMethod("trace", "POST");
		filterWithParameterForMethod("head", "POST");
		filterWithParameterForMethod("options", "POST");
	}

	@Test
	void filterWithNoParameter() throws IOException, ServletException {
		filterWithParameterForMethod(null, "POST");
	}

	private void filterWithParameterForMethod(String methodParam, String expectedMethod)
			throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		if(methodParam != null) {
			request.addParameter("_method", methodParam);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) ->
				assertThat(((HttpServletRequest) filterRequest).getMethod())
					.as("Invalid method").isEqualTo(expectedMethod);
		this.filter.doFilter(request, response, filterChain);
	}

}
