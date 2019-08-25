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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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


	@BeforeEach
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

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseBodyEmitter.class))).isTrue();

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(SseEmitter.class))).isTrue();

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseEntity.class, ResponseBodyEmitter.class))).isTrue();

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(Flux.class, String.class))).isTrue();

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(forClassWithGenerics(ResponseEntity.class,
								forClassWithGenerics(Flux.class, String.class))))).isTrue();
	}

	@Test
	public void doesNotSupportReturnTypes() throws Exception {

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseEntity.class, String.class))).isFalse();

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(forClassWithGenerics(ResponseEntity.class,
						forClassWithGenerics(AtomicReference.class, String.class))))).isFalse();

		assertThat(this.handler.supportsReturnType(
				on(TestController.class).resolveReturnType(ResponseEntity.class))).isFalse();
	}

	@Test
	public void responseBodyEmitter() throws Exception {
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseBodyEmitter.class);
		ResponseBodyEmitter emitter = new ResponseBodyEmitter();
		this.handler.handleReturnValue(emitter, type, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("");

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

		assertThat(this.response.getContentAsString()).isEqualTo(("{\"id\":1,\"name\":\"Joe\"}\n" +
						"{\"id\":2,\"name\":\"John\"}\n" +
						"{\"id\":3,\"name\":\"Jason\"}"));

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		assertThat(asyncContext.getDispatchedPath()).isNull();

		emitter.complete();
		assertThat(asyncContext.getDispatchedPath()).isNotNull();
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

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(200);

		SimpleBean bean1 = new SimpleBean();
		bean1.setId(1L);
		bean1.setName("Joe");

		SimpleBean bean2 = new SimpleBean();
		bean2.setId(2L);
		bean2.setName("John");

		emitter.send(SseEmitter.event().
				comment("a test").name("update").id("1").reconnectTime(5000L).data(bean1).data(bean2));

		assertThat(this.response.getContentType()).isEqualTo("text/event-stream;charset=UTF-8");
		assertThat(this.response.getContentAsString()).isEqualTo((":a test\n" +
						"event:update\n" +
						"id:1\n" +
						"retry:5000\n" +
						"data:{\"id\":1,\"name\":\"Joe\"}\n" +
						"data:{\"id\":2,\"name\":\"John\"}\n" +
						"\n"));
	}

	@Test
	public void responseBodyFlux() throws Exception {

		this.request.addHeader("Accept", "text/event-stream");

		MethodParameter type = on(TestController.class).resolveReturnType(Flux.class, String.class);
		EmitterProcessor<String> processor = EmitterProcessor.create();
		this.handler.handleReturnValue(processor, type, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(200);

		processor.onNext("foo");
		processor.onNext("bar");
		processor.onNext("baz");
		processor.onComplete();

		assertThat(this.response.getContentType()).isEqualTo("text/event-stream;charset=UTF-8");
		assertThat(this.response.getContentAsString()).isEqualTo("data:foo\n\ndata:bar\n\ndata:baz\n\n");
	}

	@Test // gh-21972
	public void responseBodyFluxWithError() throws Exception {

		this.request.addHeader("Accept", "text/event-stream");

		MethodParameter type = on(TestController.class).resolveReturnType(Flux.class, String.class);
		EmitterProcessor<String> processor = EmitterProcessor.create();
		this.handler.handleReturnValue(processor, type, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();

		IllegalStateException ex = new IllegalStateException("wah wah");
		processor.onError(ex);
		processor.onComplete();

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.webRequest);
		assertThat(asyncManager.getConcurrentResult()).isSameAs(ex);
		assertThat(this.response.getContentType()).isNull();
	}

	@Test
	public void responseEntitySse() throws Exception {
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseEntity.class, SseEmitter.class);
		SseEmitter emitter = new SseEmitter();
		ResponseEntity<SseEmitter> entity = ResponseEntity.ok().header("foo", "bar").body(emitter);
		this.handler.handleReturnValue(entity, type, this.mavContainer, this.webRequest);
		emitter.complete();

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentType()).isEqualTo("text/event-stream;charset=UTF-8");
		assertThat(this.response.getHeader("foo")).isEqualTo("bar");
	}

	@Test
	public void responseEntitySseNoContent() throws Exception {
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseEntity.class, SseEmitter.class);
		ResponseEntity<?> entity = ResponseEntity.noContent().header("foo", "bar").build();
		this.handler.handleReturnValue(entity, type, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isFalse();
		assertThat(this.response.getStatus()).isEqualTo(204);
		assertThat(this.response.getHeaders("foo")).isEqualTo(Collections.singletonList("bar"));
	}

	@Test
	public void responseEntityFlux() throws Exception {

		EmitterProcessor<String> processor = EmitterProcessor.create();
		ResponseEntity<Flux<String>> entity = ResponseEntity.ok().body(processor);
		ResolvableType bodyType = forClassWithGenerics(Flux.class, String.class);
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseEntity.class, bodyType);
		this.handler.handleReturnValue(entity, type, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(200);

		processor.onNext("foo");
		processor.onNext("bar");
		processor.onNext("baz");
		processor.onComplete();

		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentAsString()).isEqualTo("foobarbaz");
	}

	@Test // SPR-17076
	public void responseEntityFluxWithCustomHeader() throws Exception {

		EmitterProcessor<SimpleBean> processor = EmitterProcessor.create();
		ResponseEntity<Flux<SimpleBean>> entity = ResponseEntity.ok().header("x-foo", "bar").body(processor);
		ResolvableType bodyType = forClassWithGenerics(Flux.class, SimpleBean.class);
		MethodParameter type = on(TestController.class).resolveReturnType(ResponseEntity.class, bodyType);
		this.handler.handleReturnValue(entity, type, this.mavContainer, this.webRequest);

		assertThat(this.request.isAsyncStarted()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getHeader("x-foo")).isEqualTo("bar");
		assertThat(this.response.isCommitted()).isFalse();
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

		private ResponseEntity<Flux<SimpleBean>> h10() { return null; }
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
