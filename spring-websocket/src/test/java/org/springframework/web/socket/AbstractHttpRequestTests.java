/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	protected void setup() {
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
