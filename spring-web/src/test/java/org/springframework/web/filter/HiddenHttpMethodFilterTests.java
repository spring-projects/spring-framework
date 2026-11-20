/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
