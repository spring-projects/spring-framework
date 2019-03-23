/*
 * Copyright 2002-2015 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.handler.HandlerMappingTests}.
 * @author Brian Clozel
 */
public class HandlerMappingTests {

	private MockHttpServletRequest request;
	private AbstractHandlerMapping handlerMapping;
	private StaticWebApplicationContext context;

	@Before
	public void setup() {
		this.context = new StaticWebApplicationContext();
		this.handlerMapping = new TestHandlerMapping();
		this.request = new MockHttpServletRequest();
	}

	@Test
	public void orderedInterceptors() throws Exception {
		MappedInterceptor firstMappedInterceptor = new MappedInterceptor(new String[]{"/**"}, Mockito.mock(HandlerInterceptor.class));
		HandlerInterceptor secondHandlerInterceptor = Mockito.mock(HandlerInterceptor.class);
		MappedInterceptor thirdMappedInterceptor = new MappedInterceptor(new String[]{"/**"}, Mockito.mock(HandlerInterceptor.class));
		HandlerInterceptor fourthHandlerInterceptor = Mockito.mock(HandlerInterceptor.class);

		this.handlerMapping.setInterceptors(new Object[]{firstMappedInterceptor, secondHandlerInterceptor,
				thirdMappedInterceptor, fourthHandlerInterceptor});
		this.handlerMapping.setApplicationContext(this.context);
		HandlerExecutionChain chain = this.handlerMapping.getHandlerExecutionChain(new SimpleHandler(), this.request);
		Assert.assertThat(chain.getInterceptors(),
				Matchers.arrayContaining(firstMappedInterceptor.getInterceptor(), secondHandlerInterceptor,
						thirdMappedInterceptor.getInterceptor(), fourthHandlerInterceptor));
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
