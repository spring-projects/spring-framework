/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket;

import org.junit.Before;
import org.springframework.http.server.AsyncServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;


/**
 * @author Rossen Stoyanchev
 */
public class AbstractHttpRequestTests {

	protected ServerHttpRequest request;

	protected ServerHttpResponse response;

	protected MockHttpServletRequest servletRequest;

	protected MockHttpServletResponse servletResponse;


	@Before
	public void setUp() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletResponse = new MockHttpServletResponse();
		this.request = new AsyncServletServerHttpRequest(this.servletRequest, this.servletResponse);
		this.response = new ServletServerHttpResponse(this.servletResponse);
	}


	protected void setRequest(String method, String requestUri) {
		this.servletRequest.setMethod(method);
		this.servletRequest.setRequestURI(requestUri);
	}

	protected void resetResponse() {
		this.servletResponse = new MockHttpServletResponse();
		this.response = new ServletServerHttpResponse(this.servletResponse);
	}

}
