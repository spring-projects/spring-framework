/*
 * Copyright 2002-2017 the original author or authors.
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

import static org.junit.Assert.*;

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
		assertTrue(this.handler.supportsReturnType(returnType("handleDeferredResult")));
		assertTrue(this.handler.supportsReturnType(returnType("handleListenableFuture")));
		assertTrue(this.handler.supportsReturnType(returnType("handleCompletableFuture")));
		assertFalse(this.handler.supportsReturnType(returnType("handleString")));
	}

	@Test
	public void deferredResult() throws Exception {
		MethodParameter returnType = returnType("handleDeferredResult");
		DeferredResult<String> deferredResult = new DeferredResult<>();
		handleReturnValue(deferredResult, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		deferredResult.setResult("foo");
		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertEquals("foo", WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());
	}

	@Test
	public void deferredResultWitError() throws Exception {
		MethodParameter returnType = returnType("handleDeferredResult");
		DeferredResult<String> deferredResult = new DeferredResult<>();
		handleReturnValue(deferredResult, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		IllegalStateException ex = new IllegalStateException();
		deferredResult.setErrorResult(ex);
		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertSame(ex, WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());
	}

	@Test
	public void listenableFuture() throws Exception {
		MethodParameter returnType = returnType("handleListenableFuture");
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		handleReturnValue(future, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		future.set("foo");
		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertEquals("foo", WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());
	}

	@Test
	public void listenableFutureWithError() throws Exception {
		MethodParameter returnType = returnType("handleListenableFuture");
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		handleReturnValue(future, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		IllegalStateException ex = new IllegalStateException();
		future.setException(ex);
		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertSame(ex, WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());
	}

	@Test
	public void completableFuture() throws Exception {
		MethodParameter returnType = returnType("handleCompletableFuture");
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		handleReturnValue(future, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		future.set("foo");
		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertEquals("foo", WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());
	}

	@Test
	public void completableFutureWithError() throws Exception {
		MethodParameter returnType = returnType("handleCompletableFuture");
		CompletableFuture<String> future = new CompletableFuture<>();
		handleReturnValue(future, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		IllegalStateException ex = new IllegalStateException();
		future.completeExceptionally(ex);
		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertSame(ex, WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());
	}


	private void handleReturnValue(Object returnValue, MethodParameter returnType) throws Exception {
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		this.handler.handleReturnValue(returnValue, returnType, mavContainer, this.webRequest);
	}

	private MethodParameter returnType(String methodName) throws NoSuchMethodException {
		Method method = TestController.class.getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}


	@SuppressWarnings("unused")
	private static class TestController {

		private String handleString() {
			return null;
		}

		private DeferredResult<String> handleDeferredResult() {
			return null;
		}

		private ListenableFuture<String> handleListenableFuture() {
			return null;
		}

		private CompletableFuture<String> handleCompletableFuture() {
			return null;
		}


	}

}
