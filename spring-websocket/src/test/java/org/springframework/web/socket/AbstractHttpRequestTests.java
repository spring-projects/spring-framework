/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.socket;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

/**
 * Base class for tests using {@link ServerHttpRequest} and {@link ServerHttpResponse}.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractHttpRequestTests {

	protected ServerHttpRequest request;

	protected ServerHttpResponse response;

	protected MockHttpServletRequest servletRequest;

	protected MockHttpServletResponse servletResponse;

	protected ServerHttpAsyncRequestControl asyncControl;


	@BeforeEach
	public void setup() {
		resetRequestAndResponse();
	}

	protected void setRequest(String method, String requestUri) {
		this.servletRequest.setMethod(method);
		this.servletRequest.setRequestURI(requestUri);
		this.request = new ServletServerHttpRequest(this.servletRequest);
	}

	protected void resetRequestAndResponse() {
		resetRequest();
		resetResponse();
		this.asyncControl = this.request.getAsyncRequestControl(this.response);
	}

	protected void resetRequest() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletRequest.setAsyncSupported(true);
		this.request = new ServletServerHttpRequest(this.servletRequest);
	}

	protected void resetResponse() {
		this.servletResponse = new MockHttpServletResponse();
		this.response = new ServletServerHttpResponse(this.servletResponse);
	}

}
