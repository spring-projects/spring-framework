/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.method.ResolvableMethod.on;

/**
 * Unit tests for {@link DeferredResultMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class DeferredResultReturnValueHandlerTests {

	private DeferredResultMethodReturnValueHandler handler;

	private MockHttpServletRequest request;

	private NativeWebRequest webRequest;


	@Before
	public void setup() throws Exception {
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

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(DeferredResult.class, String.class)));

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ListenableFuture.class, String.class)));

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(CompletableFuture.class, String.class)));
	}

	@Test
	public void doesNotSupportReturnType() throws Exception {
		assertFalse(this.handler.supportsReturnType(on(TestController.class).resolveReturnType(String.class)));
	}

	@Test
	public void deferredResult() throws Exception {
		DeferredResult<String> result = new DeferredResult<>();
		IllegalStateException ex = new IllegalStateException();
		testHandle(result, DeferredResult.class, () -> result.setErrorResult(ex), ex);
	}

	@Test
	public void listenableFuture() throws Exception {
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		testHandle(future, ListenableFuture.class, () -> future.set("foo"), "foo");
	}

	@Test
	public void completableFuture() throws Exception {
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		testHandle(future, CompletableFuture.class, () -> future.set("foo"), "foo");
	}

	@Test
	public void deferredResultWitError() throws Exception {
		DeferredResult<String> result = new DeferredResult<>();
		testHandle(result, DeferredResult.class, () -> result.setResult("foo"), "foo");
	}

	@Test
	public void listenableFutureWithError() throws Exception {
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		IllegalStateException ex = new IllegalStateException();
		testHandle(future, ListenableFuture.class, () -> future.setException(ex), ex);
	}

	@Test
	public void completableFutureWithError() throws Exception {
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		IllegalStateException ex = new IllegalStateException();
		testHandle(future, CompletableFuture.class, () -> future.setException(ex), ex);
	}

	private void testHandle(Object returnValue, Class<?> asyncType,
			Runnable setResultTask, Object expectedValue) throws Exception {

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		MethodParameter returnType = on(TestController.class).resolveReturnType(asyncType, String.class);
		this.handler.handleReturnValue(returnValue, returnType, mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		setResultTask.run();

		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertEquals(expectedValue, WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());
	}


	@SuppressWarnings("unused")
	static class TestController {

		String handleString() { return null; }

		DeferredResult<String> handleDeferredResult() { return null; }

		ListenableFuture<String> handleListenableFuture() { return null; }

		CompletableFuture<String> handleCompletableFuture() { return null; }
	}

}
