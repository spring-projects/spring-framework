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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * Tests for {@link DeferredResultMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
class DeferredResultReturnValueHandlerTests {

	private DeferredResultMethodReturnValueHandler handler;

	private MockHttpServletRequest request;

	private NativeWebRequest webRequest;


	@BeforeEach
	void setup() throws Exception {
		this.handler = new DeferredResultMethodReturnValueHandler();
		this.request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request, response);

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, response);
		WebAsyncUtils.getAsyncManager(this.webRequest).setAsyncWebRequest(asyncWebRequest);
		this.request.setAsyncSupported(true);
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(DeferredResult.class, String.class))).isTrue();

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(CompletableFuture.class, String.class))).isTrue();
	}

	@Test
	void doesNotSupportReturnType() {
		assertThat(this.handler.supportsReturnType(on(TestController.class).resolveReturnType(String.class))).isFalse();
	}

	@Test
	void deferredResult() throws Exception {
		DeferredResult<String> result = new DeferredResult<>();
		IllegalStateException ex = new IllegalStateException();
		testHandle(result, DeferredResult.class, () -> result.setErrorResult(ex), ex);
	}

	@Test
	void completableFuture() throws Exception {
		CompletableFuture<String> future = new CompletableFuture<>();
		testHandle(future, CompletableFuture.class, () -> future.complete("foo"), "foo");
	}

	@Test
	void deferredResultWithError() throws Exception {
		DeferredResult<String> result = new DeferredResult<>();
		testHandle(result, DeferredResult.class, () -> result.setResult("foo"), "foo");
	}

	@Test
	void completableFutureWithError() throws Exception {
		CompletableFuture<String> future = new CompletableFuture<>();
		IllegalStateException ex = new IllegalStateException();
		testHandle(future, CompletableFuture.class, () -> future.completeExceptionally(ex), ex);
	}


	private void testHandle(Object returnValue, Class<?> asyncType,
			Runnable setResultTask, Object expectedValue) throws Exception {

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		MethodParameter returnType = on(TestController.class).resolveReturnType(asyncType, String.class);
		this.handler.handleReturnValue(returnValue, returnType, mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult()).isFalse();

		setResultTask.run();

		assertThat(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult()).isTrue();
		assertThat(WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult()).isEqualTo(expectedValue);
	}


	@SuppressWarnings("unused")
	static class TestController {

		String handleString() { return null; }

		DeferredResult<String> handleDeferredResult() { return null; }

		CompletableFuture<String> handleCompletableFuture() { return null; }
	}

}
