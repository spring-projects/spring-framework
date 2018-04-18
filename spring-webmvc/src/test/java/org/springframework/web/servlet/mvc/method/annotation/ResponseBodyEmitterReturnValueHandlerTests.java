/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.web.method.ResolvableMethod.on;

/**
 * Unit tests for ResponseBodyEmitterReturnValueHandler.
 * @author Rossen Stoyanchev
 */
public class ResponseBodyEmitterReturnValueHandlerTests {

	private ResponseBodyEmitterReturnValueHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private NativeWebRequest webRequest;

	private final ModelAndViewContainer mavContainer = new ModelAndViewContainer();


	@Before
	public void setup() throws Exception {

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
	public void supportsReturnTypes() throws Exception {

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseBodyEmitter.class)));

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(SseEmitter.class)));

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseEntity.class, ResponseBodyEmitter.class)));

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(Flux.class, String.class)));

		assertTrue(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(forClassWithGenerics(ResponseEntity.class,
								forClassWithGenerics(Flux.class, String.class)))));
	}

	@Test
	public void doesNotSupportReturnTypes() throws Exception {

		assertFalse(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseEntity.class, String.class)));

		assertFalse(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(forClassWithGenerics(ResponseEntity.class,
						forClassWithGenerics(AtomicReference.class, String.class)))));

		assertFalse(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseEntity.class)));
	}

	@Test
	public void responseBodyEmitter() throws Exception {
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseBodyEmitter.class);
		ResponseBodyEmitter emitter = new ResponseBodyEmitter();
		this.handler.handleReturnValue(emitter, type, this.mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertEquals("", this.response.getContentAsString());

		SimpleBean bean = new SimpleBean();
		bean.setId(1L);
		bean.setName("Joe");
		emitter.send(bean);
		emitter.send("\n");

		bean.setId(2L);
		bean.setName("John");
		emitter.send(bean);
		emitter.send("\n");

		bean.setId(3L);
		bean.setName("Jason");
		emitter.send(bean);

		assertEquals("{\"id\":1,\"name\":\"Joe\"}\n" +
						"{\"id\":2,\"name\":\"John\"}\n" +
						"{\"id\":3,\"name\":\"Jason\"}", this.response.getContentAsString());

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		assertNull(asyncContext.getDispatchedPath());

		emitter.complete();
		assertNotNull(asyncContext.getDispatchedPath());
	}

	@Test
	public void responseBodyEmitterWithTimeoutValue() throws Exception {

		AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
		WebAsyncUtils.getAsyncManager(this.request).setAsyncWebRequest(asyncWebRequest);

		ResponseBodyEmitter emitter = new ResponseBodyEmitter(19000L);
		emitter.onTimeout(mock(Runnable.class));
		emitter.onCompletion(mock(Runnable.class));

		MethodParameter type = on(TestController.class).resolveReturnType(ResponseBodyEmitter.class);
		this.handler.handleReturnValue(emitter, type, this.mavContainer, this.webRequest);

		verify(asyncWebRequest).setTimeout(19000L);
		verify(asyncWebRequest).addTimeoutHandler(any(Runnable.class));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).startAsync();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void responseBodyEmitterWithErrorValue() throws Exception {

		AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
		WebAsyncUtils.getAsyncManager(this.request).setAsyncWebRequest(asyncWebRequest);

		ResponseBodyEmitter emitter = new ResponseBodyEmitter(19000L);
		emitter.onError(mock(Consumer.class));
		emitter.onCompletion(mock(Runnable.class));

		MethodParameter type = on(TestController.class).resolveReturnType(ResponseBodyEmitter.class);
		this.handler.handleReturnValue(emitter, type, this.mavContainer, this.webRequest);

		verify(asyncWebRequest).addErrorHandler(any(Consumer.class));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).startAsync();
	}

	@Test
	public void sseEmitter() throws Exception {
		MethodParameter type = on(TestController.class).resolveReturnType(SseEmitter.class);
		SseEmitter emitter = new SseEmitter();
		this.handler.handleReturnValue(emitter, type, this.mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertEquals(200, this.response.getStatus());
		assertEquals("text/event-stream;charset=UTF-8", this.response.getContentType());

		SimpleBean bean1 = new SimpleBean();
		bean1.setId(1L);
		bean1.setName("Joe");

		SimpleBean bean2 = new SimpleBean();
		bean2.setId(2L);
		bean2.setName("John");

		emitter.send(SseEmitter.event().
				comment("a test").name("update").id("1").reconnectTime(5000L).data(bean1).data(bean2));

		assertEquals(":a test\n" +
						"event:update\n" +
						"id:1\n" +
						"retry:5000\n" +
						"data:{\"id\":1,\"name\":\"Joe\"}\n" +
						"data:{\"id\":2,\"name\":\"John\"}\n" +
						"\n", this.response.getContentAsString());
	}

	@Test
	public void responseBodyFlux() throws Exception {

		this.request.addHeader("Accept", "text/event-stream");

		MethodParameter type = on(TestController.class).resolveReturnType(Flux.class, String.class);
		EmitterProcessor<String> processor = EmitterProcessor.create();
		this.handler.handleReturnValue(processor, type, this.mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertEquals(200, this.response.getStatus());
		assertEquals("text/event-stream;charset=UTF-8", this.response.getContentType());

		processor.onNext("foo");
		processor.onNext("bar");
		processor.onNext("baz");
		processor.onComplete();

		assertEquals("data:foo\n\ndata:bar\n\ndata:baz\n\n", this.response.getContentAsString());
	}

	@Test
	public void responseEntitySse() throws Exception {
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseEntity.class, SseEmitter.class);
		ResponseEntity<SseEmitter> entity = ResponseEntity.ok().header("foo", "bar").body(new SseEmitter());
		this.handler.handleReturnValue(entity, type, this.mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertEquals(200, this.response.getStatus());
		assertEquals("text/event-stream;charset=UTF-8", this.response.getContentType());
		assertEquals("bar", this.response.getHeader("foo"));
	}

	@Test
	public void responseEntitySseNoContent() throws Exception {
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseEntity.class, SseEmitter.class);
		ResponseEntity<?> entity = ResponseEntity.noContent().header("foo", "bar").build();
		this.handler.handleReturnValue(entity, type, this.mavContainer, this.webRequest);

		assertFalse(this.request.isAsyncStarted());
		assertEquals(204, this.response.getStatus());
		assertEquals(Collections.singletonList("bar"), this.response.getHeaders("foo"));
	}

	@Test
	public void responseEntityFlux() throws Exception {

		EmitterProcessor<String> processor = EmitterProcessor.create();
		ResponseEntity<Flux<String>> entity = ResponseEntity.ok().body(processor);
		ResolvableType bodyType = forClassWithGenerics(Flux.class, String.class);
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseEntity.class, bodyType);
		this.handler.handleReturnValue(entity, type, this.mavContainer, this.webRequest);

		assertTrue(this.request.isAsyncStarted());
		assertEquals(200, this.response.getStatus());
		assertEquals("text/plain", this.response.getContentType());

		processor.onNext("foo");
		processor.onNext("bar");
		processor.onNext("baz");
		processor.onComplete();

		assertEquals("foobarbaz", this.response.getContentAsString());
	}


	@SuppressWarnings("unused")
	private static class TestController {

		private ResponseBodyEmitter h1() { return null; }

		private ResponseEntity<ResponseBodyEmitter> h2() { return null; }

		private SseEmitter h3() { return null; }

		private ResponseEntity<SseEmitter> h4() { return null; }

		private ResponseEntity<String> h5() { return null; }

		private ResponseEntity<AtomicReference<String>> h6() { return null; }

		private ResponseEntity<?> h7() { return null; }

		private Flux<String> h8() { return null; }

		private ResponseEntity<Flux<String>> h9() { return null; }

	}


	@SuppressWarnings("unused")
	private static class SimpleBean {

		private Long id;

		private String name;

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
