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

package org.springframework.web.servlet.function.support;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.mock;

/**
 * Unit tests for {@link HandlerFunctionAdapter}.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerFunctionAdapterTests {

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");

	private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	private final HandlerFunctionAdapter adapter = new HandlerFunctionAdapter();


	@BeforeEach
	void setUp() {
		this.servletRequest.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE,
				ServerRequest.create(this.servletRequest, List.of(new StringHttpMessageConverter())));
	}


	@Test
	void asyncRequestNotUsable() throws Exception {

		HandlerFunction<?> handler = request -> ServerResponse.sse(sseBuilder -> {
			try {
				sseBuilder.data("data 1");
				sseBuilder.data("data 2");
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});

		this.servletRequest.setAsyncSupported(true);

		HttpServletResponse mockServletResponse = mock(HttpServletResponse.class);
		doThrow(new IOException("Broken pipe")).when(mockServletResponse).getOutputStream();

		// Use of response should be rejected
		assertThatThrownBy(() -> adapter.handle(servletRequest, mockServletResponse, handler))
				.hasRootCauseInstanceOf(IOException.class)
				.hasRootCauseMessage("Broken pipe");
	}

	@Test
	void asyncRequestNotUsableOnAsyncDispatch() throws Exception {

		HandlerFunction<?> handler = request -> ServerResponse.ok().body("body");

		// Put AsyncWebRequest in ERROR state
		StandardServletAsyncWebRequest asyncRequest = new StandardServletAsyncWebRequest(servletRequest, servletResponse);
		asyncRequest.onError(new AsyncEvent(new MockAsyncContext(servletRequest, servletResponse), new Exception()));

		// Set it as the current AsyncWebRequest, from the initial REQUEST dispatch
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);
		asyncManager.setAsyncWebRequest(asyncRequest);

		// Use of response should be rejected
		assertThatThrownBy(() -> adapter.handle(servletRequest, servletResponse, handler))
				.isInstanceOf(AsyncRequestNotUsableException.class);
	}

}
