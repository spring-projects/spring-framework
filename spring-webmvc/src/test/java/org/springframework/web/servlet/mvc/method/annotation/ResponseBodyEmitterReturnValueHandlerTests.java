/*
 * Copyright 2002-2016 the original author or authors.
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
import static org.mockito.Mockito.*;
import static org.springframework.web.servlet.mvc.method.annotation.SseEmitter.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.test.MockAsyncContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;


/**
 * Unit tests for ResponseBodyEmitterReturnValueHandler.
 * @author Rossen Stoyanchev
 */
public class ResponseBodyEmitterReturnValueHandlerTests {

	private ResponseBodyEmitterReturnValueHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private NativeWebRequest webRequest;


	@Before
	public void setUp() throws Exception {

		List<HttpMessageConverter<?>> converters = Arrays.asList(
				new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter());

		this.handler = new ResponseBodyEmitterReturnValueHandler(converters);
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request, this.response);

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncUtils.getAsyncManager(this.webRequest).setAsyncWebRequest(asyncWebRequest);
		this.request.setAsyncSupported(true);
	}

	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(returnType("handle")));
		assertTrue(this.handler.supportsReturnType(returnType("handleSse")));
		assertTrue(this.handler.supportsReturnType(returnType("handleResponseEntity")));
		assertFalse(this.handler.supportsReturnType(returnType("handleResponseEntityString")));
		assertFalse(this.handler.supportsReturnType(returnType("handleResponseEntityParameterized")));
		assertTrue(this.handler.supportsReturnType(returnType("handleObservable")));
		assertTrue(this.handler.supportsReturnType(returnType("handleObservableSse")));
	}

	@Test
	public void responseBodyEmitter() throws Exception {
		MethodParameter returnType = returnType("handle");
		ResponseBodyEmitter emitter = new ResponseBodyEmitter();
		handleReturnValue(emitter, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertEquals("", this.response.getContentAsString());

		emitter.send(new SimpleBean(1L, "Joe"));
		emitter.send("\n");

		emitter.send(new SimpleBean(2L, "John"));
		emitter.send("\n");

		emitter.send(new SimpleBean(3L, "Jason"));

		assertEquals("{\"id\":1,\"name\":\"Joe\"}\n" +
						"{\"id\":2,\"name\":\"John\"}\n" +
						"{\"id\":3,\"name\":\"Jason\"}",
				this.response.getContentAsString());

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		assertNull(asyncContext.getDispatchedPath());

		emitter.complete();
		assertNotNull(asyncContext.getDispatchedPath());
	}

	@Test
	public void timeoutValueAndCallback() throws Exception {

		AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
		WebAsyncUtils.getAsyncManager(this.request).setAsyncWebRequest(asyncWebRequest);

		ResponseBodyEmitter emitter = new ResponseBodyEmitter(19000L);
		emitter.onTimeout(mock(Runnable.class));
		emitter.onCompletion(mock(Runnable.class));

		MethodParameter returnType = returnType("handle");
		handleReturnValue(emitter, returnType);

		verify(asyncWebRequest).setTimeout(19000L);
		verify(asyncWebRequest).addTimeoutHandler(any(Runnable.class));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).startAsync();
	}

	@Test
	public void sseEmitter() throws Exception {
		MethodParameter returnType = returnType("handleSse");
		SseEmitter emitter = new SseEmitter();
		handleReturnValue(emitter, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertEquals(200, this.response.getStatus());
		assertEquals("text/event-stream", this.response.getContentType());

		SimpleBean bean1 = new SimpleBean(1L, "Joe");
		SimpleBean bean2 = new SimpleBean(2L, "John");

		emitter.send(event().comment("a test").name("update").id("1").reconnectTime(5000L).data(bean1).data(bean2));

		assertEquals(":a test\n" +
						"event:update\n" +
						"id:1\n" +
						"retry:5000\n" +
						"data:{\"id\":1,\"name\":\"Joe\"}\n" +
						"data:{\"id\":2,\"name\":\"John\"}\n" +
						"\n",
				this.response.getContentAsString());
	}

	@Test
	public void responseEntitySse() throws Exception {
		MethodParameter returnType = returnType("handleResponseEntitySse");
		ResponseEntity<SseEmitter> entity = ResponseEntity.ok().header("foo", "bar").body(new SseEmitter());
		handleReturnValue(entity, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertEquals(200, this.response.getStatus());
		assertEquals("text/event-stream", this.response.getContentType());
		assertEquals("bar", this.response.getHeader("foo"));
	}

	@Test
	public void responseEntitySseNoContent() throws Exception {
		MethodParameter returnType = returnType("handleResponseEntitySse");
		ResponseEntity<?> entity = ResponseEntity.noContent().build();
		handleReturnValue(entity, returnType);

		assertFalse(this.request.isAsyncStarted());
		assertEquals(204, this.response.getStatus());
	}

	@Test
	public void observable() throws Exception {
		SimpleBean bean1 = new SimpleBean(1L, "Joe");
		SimpleBean bean2 = new SimpleBean(2L, "John");
		SimpleBean bean3 = new SimpleBean(3L, "Jason");

		MethodParameter returnType = returnType("handleObservable");
		Observable<SimpleBean> observable = Observable.just(bean1, bean2, bean3);
		handleReturnValue(observable, returnType);

		assertTrue(this.request.isAsyncStarted());
		String json = "{\"id\":1,\"name\":\"Joe\"}{\"id\":2,\"name\":\"John\"}{\"id\":3,\"name\":\"Jason\"}";
		assertEquals(json, this.response.getContentAsString());
		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		assertNotNull(asyncContext.getDispatchedPath());
	}

	@Test
	public void observableWithSse() throws Exception {
		SimpleBean bean1 = new SimpleBean(1L, "Joe");
		SimpleBean bean2 = new SimpleBean(2L, "John");
		SimpleBean bean3 = new SimpleBean(3L, "Jason");

		MethodParameter returnType = returnType("handleObservableSse");
		ResponseEntity<Observable<SimpleBean>> entity = ResponseEntity.ok()
				.contentType(new MediaType("text", "event-stream"))
				.body(Observable.just(bean1, bean2, bean3));
		handleReturnValue(entity, returnType);

		assertTrue(this.request.isAsyncStarted());
		assertEquals("data:{\"id\":1,\"name\":\"Joe\"}\n\n" +
				"data:{\"id\":2,\"name\":\"John\"}\n\n" +
				"data:{\"id\":3,\"name\":\"Jason\"}\n\n", this.response.getContentAsString());
		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		assertNotNull(asyncContext.getDispatchedPath());
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

		private ResponseBodyEmitter handle() {
			return null;
		}

		private ResponseEntity<ResponseBodyEmitter> handleResponseEntity() {
			return null;
		}

		private SseEmitter handleSse() {
			return null;
		}

		private ResponseEntity<SseEmitter> handleResponseEntitySse() {
			return null;
		}

		private ResponseEntity<String> handleResponseEntityString() {
			return null;
		}

		private ResponseEntity<AtomicReference<String>> handleResponseEntityParameterized() {
			return null;
		}

		private Observable<SimpleBean> handleObservable() {
			return null;
		}

		private ResponseEntity<Observable<SimpleBean>> handleObservableSse() {
			return null;
		}
	}

	private static class SimpleBean {

		private Long id;

		private String name;

		public SimpleBean(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
