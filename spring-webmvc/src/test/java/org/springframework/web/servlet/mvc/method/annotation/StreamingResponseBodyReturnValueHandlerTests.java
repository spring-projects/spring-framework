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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link StreamingResponseBodyReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
class StreamingResponseBodyReturnValueHandlerTests {

	private StreamingResponseBodyReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	void setup() throws Exception {
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
	void supportsReturnType() throws Exception {
		assertThat(this.handler.supportsReturnType(returnType(TestController.class, "handle"))).isTrue();
		assertThat(this.handler.supportsReturnType(returnType(TestController.class, "handleResponseEntity"))).isTrue();
		assertThat(this.handler.supportsReturnType(returnType(TestController.class, "handleResponseEntityString"))).isFalse();
		assertThat(this.handler.supportsReturnType(returnType(TestController.class, "handleResponseEntityParameterized"))).isFalse();
	}

	@Test
	void streamingResponseBody() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);

		MethodParameter returnType = returnType(TestController.class, "handle");
		StreamingResponseBody streamingBody = outputStream -> {
			outputStream.write("foo".getBytes(StandardCharsets.UTF_8));
			latch.countDown();
		};
		this.handler.handleReturnValue(streamingBody, returnType, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("foo");
	}


	@Test
	void responseEntity() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);

		MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
		ResponseEntity<StreamingResponseBody> emitter = ResponseEntity.ok().header("foo", "bar")
				.body(outputStream -> {
					outputStream.write("foo".getBytes(StandardCharsets.UTF_8));
					latch.countDown();
				});
		this.handler.handleReturnValue(emitter, returnType, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getHeader("foo")).isEqualTo("bar");

		assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("foo");

	}

	@Test
	void responseEntityNoContent() throws Exception {
		MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
		ResponseEntity<?> emitter = ResponseEntity.noContent().build();
		this.handler.handleReturnValue(emitter, returnType, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isFalse();
		assertThat(this.response.getStatus()).isEqualTo(204);
	}

	@Test
	void responseEntityWithHeadersAndNoContent() throws Exception {
		ResponseEntity<?> emitter = ResponseEntity.noContent().header("foo", "bar").build();
		MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
		this.handler.handleReturnValue(emitter, returnType, this.mavContainer, this.webRequest);

		assertThat(this.response.getHeaders("foo")).isEqualTo(Collections.singletonList("bar"));
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
