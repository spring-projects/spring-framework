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

package org.springframework.web.context.request;

import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class ServletWebRequestTests {

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;


	@BeforeEach
	void setup() {
		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		request = new ServletWebRequest(servletRequest, servletResponse);
	}


	@Test
	void parameters() {
		servletRequest.addParameter("param1", "value1");
		servletRequest.addParameter("param2", "value2");
		servletRequest.addParameter("param2", "value2a");

		assertThat(request.getParameter("param1")).isEqualTo("value1");
		assertThat(request.getParameterValues("param1")).containsExactly("value1");
		assertThat(request.getParameter("param2")).isEqualTo("value2");
		assertThat(request.getParameterValues("param2")).containsExactly("value2", "value2a");

		Map<String, String[]> paramMap = request.getParameterMap();
		assertThat(paramMap).hasSize(2);
		assertThat(paramMap.get("param1")).hasSize(1);
		assertThat(paramMap.get("param1")[0]).isEqualTo("value1");
		assertThat(paramMap.get("param2")).hasSize(2);
		assertThat(paramMap.get("param2")[0]).isEqualTo("value2");
		assertThat(paramMap.get("param2")[1]).isEqualTo("value2a");
	}

	@Test
	void locale() {
		servletRequest.addPreferredLocale(Locale.UK);

		assertThat(request.getLocale()).isEqualTo(Locale.UK);
	}

	@Test
	void nativeRequest() {
		assertThat(request.getNativeRequest()).isSameAs(servletRequest);
		assertThat(request.getNativeRequest(ServletRequest.class)).isSameAs(servletRequest);
		assertThat(request.getNativeRequest(HttpServletRequest.class)).isSameAs(servletRequest);
		assertThat(request.getNativeRequest(MockHttpServletRequest.class)).isSameAs(servletRequest);
		assertThat(request.getNativeRequest(MultipartRequest.class)).isNull();
		assertThat(request.getNativeResponse()).isSameAs(servletResponse);
		assertThat(request.getNativeResponse(ServletResponse.class)).isSameAs(servletResponse);
		assertThat(request.getNativeResponse(HttpServletResponse.class)).isSameAs(servletResponse);
		assertThat(request.getNativeResponse(MockHttpServletResponse.class)).isSameAs(servletResponse);
		assertThat(request.getNativeResponse(MultipartRequest.class)).isNull();
	}

	@Test
	void decoratedNativeRequest() {
		HttpServletRequest decoratedRequest = new HttpServletRequestWrapper(servletRequest);
		HttpServletResponse decoratedResponse = new HttpServletResponseWrapper(servletResponse);
		ServletWebRequest request = new ServletWebRequest(decoratedRequest, decoratedResponse);
		assertThat(request.getNativeRequest()).isSameAs(decoratedRequest);
		assertThat(request.getNativeRequest(ServletRequest.class)).isSameAs(decoratedRequest);
		assertThat(request.getNativeRequest(HttpServletRequest.class)).isSameAs(decoratedRequest);
		assertThat(request.getNativeRequest(MockHttpServletRequest.class)).isSameAs(servletRequest);
		assertThat(request.getNativeRequest(MultipartRequest.class)).isNull();
		assertThat(request.getNativeResponse()).isSameAs(decoratedResponse);
		assertThat(request.getNativeResponse(ServletResponse.class)).isSameAs(decoratedResponse);
		assertThat(request.getNativeResponse(HttpServletResponse.class)).isSameAs(decoratedResponse);
		assertThat(request.getNativeResponse(MockHttpServletResponse.class)).isSameAs(servletResponse);
		assertThat(request.getNativeResponse(MultipartRequest.class)).isNull();
	}

}
