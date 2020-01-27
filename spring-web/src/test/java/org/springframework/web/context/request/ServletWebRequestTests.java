/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class ServletWebRequestTests {

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;


	@BeforeEach
	public void setup() {
		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		request = new ServletWebRequest(servletRequest, servletResponse);
	}


	@Test
	public void parameters() {
		servletRequest.addParameter("param1", "value1");
		servletRequest.addParameter("param2", "value2");
		servletRequest.addParameter("param2", "value2a");

		assertThat(request.getParameter("param1")).isEqualTo("value1");
		assertThat(request.getParameterValues("param1").length).isEqualTo(1);
		assertThat(request.getParameterValues("param1")[0]).isEqualTo("value1");
		assertThat(request.getParameter("param2")).isEqualTo("value2");
		assertThat(request.getParameterValues("param2").length).isEqualTo(2);
		assertThat(request.getParameterValues("param2")[0]).isEqualTo("value2");
		assertThat(request.getParameterValues("param2")[1]).isEqualTo("value2a");

		Map<String, String[]> paramMap = request.getParameterMap();
		assertThat(paramMap.size()).isEqualTo(2);
		assertThat(paramMap.get("param1").length).isEqualTo(1);
		assertThat(paramMap.get("param1")[0]).isEqualTo("value1");
		assertThat(paramMap.get("param2").length).isEqualTo(2);
		assertThat(paramMap.get("param2")[0]).isEqualTo("value2");
		assertThat(paramMap.get("param2")[1]).isEqualTo("value2a");
	}

	@Test
	public void locale() {
		servletRequest.addPreferredLocale(Locale.UK);

		assertThat(request.getLocale()).isEqualTo(Locale.UK);
	}

	@Test
	public void nativeRequest() {
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
	public void decoratedNativeRequest() {
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
