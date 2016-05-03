/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;


/**
 * Unit tests for
 * {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBodyReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class StreamingResponseBodyReturnValueHandlerTests {

	private StreamingResponseBodyReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setUp() throws Exception {

		this.handler = new StreamingResponseBodyReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();

		this.request = new MockHttpServletRequest("GET", "/path");
		this.response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request, this.response);

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncUtils.getAsyncManager(this.webRequest).setAsyncWebRequest(asyncWebRequest);
		this.request.setAsyncSupported(true);
	}

	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(returnType(TestController.class, "handle")));
		assertTrue(this.handler.supportsReturnType(returnType(TestController.class, "handleResponseEntity")));
		assertFalse(this.handler.supportsReturnType(returnType(TestController.class, "handleResponseEntityString")));
		assertFalse(this.handler.supportsReturnType(returnType(TestController.class, "handleResponseEntityParameterized")));
	}

	@Test
	public void streamingResponseBody() throws Exception {

		CountDownLatch latch = new CountDownLatch(1);

		MethodParameter returnType = returnType(TestController.class, "handle");
		StreamingResponseBody streamingBody = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				outputStream.write("foo".getBytes(Charset.forName("UTF-8")));
				latch.countDown();
			}
		};
		this.handler.handleReturnValue(streamingBody, returnType, this.mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertEquals("foo", this.response.getContentAsString());
	}


	@Test
	public void responseEntity() throws Exception {

		CountDownLatch latch = new CountDownLatch(1);

		MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
		ResponseEntity<StreamingResponseBody> emitter = ResponseEntity.ok().header("foo", "bar")
				.body(new StreamingResponseBody() {

					@Override
					public void writeTo(OutputStream outputStream) throws IOException {
						outputStream.write("foo".getBytes(Charset.forName("UTF-8")));
						latch.countDown();
					}
				});
		this.handler.handleReturnValue(emitter, returnType, this.mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertEquals(200, this.response.getStatus());
		assertEquals("bar", this.response.getHeader("foo"));

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertEquals("foo", this.response.getContentAsString());

	}

	@Test
	public void responseEntityNoContent() throws Exception {
		MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
		ResponseEntity<?> emitter = ResponseEntity.noContent().build();
		this.handler.handleReturnValue(emitter, returnType, this.mavContainer, this.webRequest);

		assertFalse(this.request.isAsyncStarted());
		assertEquals(204, this.response.getStatus());
	}


	private MethodParameter returnType(Class<?> clazz, String methodName) throws NoSuchMethodException {
		Method method = clazz.getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}


	@SuppressWarnings("unused")
	private static class TestController {

		private StreamingResponseBody handle() {
			return null;
		}

		private ResponseEntity<StreamingResponseBody> handleResponseEntity() {
			return null;
		}

		private ResponseEntity<String> handleResponseEntityString() {
			return null;
		}

		private ResponseEntity<AtomicReference<String>> handleResponseEntityParameterized() {
			return null;
		}
	}

}
