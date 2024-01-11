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

package org.springframework.web.servlet.view;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.View;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockRequestDispatcher;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link InternalResourceView}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class InternalResourceViewTests {

	private static final Map<String, Object> model = Map.of("foo", "bar", "I", 1L);

	private static final String url = "forward-to";

	private final HttpServletRequest request = mock();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final InternalResourceView view = new InternalResourceView();


	/**
	 * If the url property isn't supplied, view initialization should fail.
	 */
	@Test
	void rejectsNullUrl() {
		assertThatIllegalArgumentException().isThrownBy(
				view::afterPropertiesSet);
	}

	@Test
	void forward() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myservlet/handler.do");
		request.setContextPath("/mycontext");
		request.setServletPath("/myservlet");
		request.setPathInfo(";mypathinfo");
		request.setQueryString("?param1=value1");

		view.setUrl(url);
		view.setServletContext(new MockServletContext() {
			@Override
			public int getMinorVersion() {
				return 4;
			}
		});

		view.render(model, request, response);
		assertThat(response.getForwardedUrl()).isEqualTo(url);

		model.forEach((key, value) -> assertThat(request.getAttribute(key)).as("Values for model key '" + key
						+ "' must match").isEqualTo(value));
	}

	@Test
	void alwaysInclude() throws Exception {
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		view.setUrl(url);
		view.setAlwaysInclude(true);

		// Can now try multiple tests
		view.render(model, request, response);
		assertThat(response.getIncludedUrl()).isEqualTo(url);

		model.forEach((key, value) -> verify(request).setAttribute(key, value));
	}

	@Test
	void includeOnAttribute() throws Exception {
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn("somepath");
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		view.setUrl(url);

		// Can now try multiple tests
		view.render(model, request, response);
		assertThat(response.getIncludedUrl()).isEqualTo(url);

		model.forEach((key, value) -> verify(request).setAttribute(key, value));
	}

	@Test
	void includeOnCommitted() throws Exception {
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		response.setCommitted(true);
		view.setUrl(url);

		// Can now try multiple tests
		view.render(model, request, response);
		assertThat(response.getIncludedUrl()).isEqualTo(url);

		model.forEach((k, v) -> verify(request).setAttribute(k, v));
	}

}
