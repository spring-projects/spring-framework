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

package org.springframework.web.servlet.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.WebContentGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link org.springframework.web.servlet.HandlerMapping}.
 * @author Brian Clozel
 */
public class HandlerMappingTests {

	private AbstractHandlerMapping handlerMapping = new TestHandlerMapping();
	private StaticWebApplicationContext context = new StaticWebApplicationContext();
	private MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	public void orderedInterceptors() throws Exception {
		HandlerInterceptor i1 = mock(HandlerInterceptor.class);
		MappedInterceptor mappedInterceptor1 = new MappedInterceptor(new String[]{"/**"}, i1);
		HandlerInterceptor i2 = mock(HandlerInterceptor.class);
		HandlerInterceptor i3 = mock(HandlerInterceptor.class);
		MappedInterceptor mappedInterceptor3 = new MappedInterceptor(new String[]{"/**"}, i3);
		HandlerInterceptor i4 = mock(HandlerInterceptor.class);

		this.handlerMapping.setInterceptors(mappedInterceptor1, i2, mappedInterceptor3, i4);
		this.handlerMapping.setApplicationContext(this.context);
		HandlerExecutionChain chain = this.handlerMapping.getHandlerExecutionChain(new SimpleHandler(), this.request);
		assertThat(chain.getInterceptors()).contains(
				mappedInterceptor1.getInterceptor(), i2, mappedInterceptor3.getInterceptor(), i4);
	}

	class TestHandlerMapping extends AbstractHandlerMapping {

		@Override
		protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
			return new SimpleHandler();
		}
	}

	class SimpleHandler extends WebContentGenerator implements HttpRequestHandler {

		public SimpleHandler() {
			super(METHOD_GET);
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			response.setStatus(HttpStatus.OK.value());
		}

	}

}
