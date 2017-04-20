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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import rx.Single;
import rx.SingleEmitter;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.method.ResolvableMethod.on;

/**
 * Unit tests for {@link ReactiveTypeHandler}.
 * @author Rossen Stoyanchev
 */
public class ReactiveTypeHandlerTests {

	private ReactiveTypeHandler handler;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private NativeWebRequest webRequest;


	@Before
	public void setup() throws Exception {
		ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
		factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = factoryBean.getObject();
		this.handler = new ReactiveTypeHandler(new ReactiveAdapterRegistry(), new SyncTaskExecutor(), manager);
		resetRequest();
	}

	private void resetRequest() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletResponse = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.servletRequest, this.servletResponse);

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.servletRequest, this.servletResponse);
		WebAsyncUtils.getAsyncManager(this.webRequest).setAsyncWebRequest(asyncWebRequest);
		this.servletRequest.setAsyncSupported(true);
	}


	@Test
	public void supportsType() throws Exception {
		assertTrue(this.handler.isReactiveType(Mono.class));
		assertTrue(this.handler.isReactiveType(Single.class));
		assertTrue(this.handler.isReactiveType(io.reactivex.Single.class));
	}

	@Test
	public void doesNotSupportType() throws Exception {
		assertFalse(this.handler.isReactiveType(String.class));
	}

	@Test
	public void deferredResultSubscriberWithOneValue() throws Exception {

		// Mono
		MonoProcessor<String> mono = MonoProcessor.create();
		testDeferredResultSubscriber(mono, Mono.class, () -> mono.onNext("foo"), "foo");

		// Mono empty
		MonoProcessor<String> monoEmpty = MonoProcessor.create();
		testDeferredResultSubscriber(monoEmpty, Mono.class, monoEmpty::onComplete, null);

		// RxJava 1 Single
		AtomicReference<SingleEmitter<String>> ref = new AtomicReference<>();
		Single<String> single = Single.fromEmitter(ref::set);
		testDeferredResultSubscriber(single, Single.class, () -> ref.get().onSuccess("foo"), "foo");

		// RxJava 2 Single
		AtomicReference<io.reactivex.SingleEmitter<String>> ref2 = new AtomicReference<>();
		io.reactivex.Single<String> single2 = io.reactivex.Single.create(ref2::set);
		testDeferredResultSubscriber(single2, io.reactivex.Single.class, () -> ref2.get().onSuccess("foo"), "foo");
	}

	@Test
	public void deferredResultSubscriberWithNoValues() throws Exception {
		MonoProcessor<String> monoEmpty = MonoProcessor.create();
		testDeferredResultSubscriber(monoEmpty, Mono.class, monoEmpty::onComplete, null);
	}

	@Test
	public void deferredResultSubscriberWithMultipleValues() throws Exception {

		// JSON must be preferred for Flux<String> -> List<String> or else we stream
		this.servletRequest.addHeader("Accept", "application/json");

		EmitterProcessor<String> emitter = EmitterProcessor.create();
		testDeferredResultSubscriber(emitter, Flux.class, () -> {
			emitter.onNext("foo");
			emitter.onNext("bar");
			emitter.onNext("baz");
			emitter.onComplete();
		}, Arrays.asList("foo", "bar", "baz"));
	}

	@Test
	public void deferredResultSubscriberWithError() throws Exception {

		IllegalStateException ex = new IllegalStateException();

		// Mono
		MonoProcessor<String> mono = MonoProcessor.create();
		testDeferredResultSubscriber(mono, Mono.class, () -> mono.onError(ex), ex);

		// RxJava 1 Single
		AtomicReference<SingleEmitter<String>> ref = new AtomicReference<>();
		Single<String> single = Single.fromEmitter(ref::set);
		testDeferredResultSubscriber(single, Single.class, () -> ref.get().onError(ex), ex);

		// RxJava 2 Single
		AtomicReference<io.reactivex.SingleEmitter<String>> ref2 = new AtomicReference<>();
		io.reactivex.Single<String> single2 = io.reactivex.Single.create(ref2::set);
		testDeferredResultSubscriber(single2, io.reactivex.Single.class, () -> ref2.get().onError(ex), ex);
	}

	@Test
	public void jsonArrayOfStrings() throws Exception {

		// Empty -> null
		testJsonNotPreferred("text/plain");
		testJsonNotPreferred("text/plain, application/json");
		testJsonNotPreferred("text/markdown");
		testJsonNotPreferred("foo/bar");

		// Empty -> List[0] when JSON is preferred
		testJsonPreferred("application/json");
		testJsonPreferred("application/foo+json");
		testJsonPreferred("application/json, text/plain");
		testJsonPreferred("*/*, application/json, text/plain");
	}

	private void testJsonNotPreferred(String acceptHeaderValue) throws Exception {
		resetRequest();
		this.servletRequest.addHeader("Accept", acceptHeaderValue);
		EmitterProcessor<String> processor = EmitterProcessor.create();
		ResponseBodyEmitter emitter = handleValue(processor, Flux.class);
		assertNotNull(emitter);
	}

	private void testJsonPreferred(String acceptHeaderValue) throws Exception {
		resetRequest();
		this.servletRequest.addHeader("Accept", acceptHeaderValue);
		EmitterProcessor<String> processor = EmitterProcessor.create();
		testDeferredResultSubscriber(processor, Flux.class, processor::onComplete, Collections.emptyList());
	}

	@Test
	public void mediaTypes() throws Exception {

		// Media type from request
		this.servletRequest.addHeader("Accept", "text/event-stream");
		testSseResponse(true);

		// Media type from "produces" attribute
		Set<MediaType> types = Collections.singleton(MediaType.TEXT_EVENT_STREAM);
		this.servletRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, types);
		testSseResponse(true);

		// No media type preferences
		testSseResponse(false);

		// Requested media types are sorted
		testJsonPreferred("text/plain;q=0.8, application/json;q=1.0");
		testJsonNotPreferred("text/plain, application/json");
	}

	private void testSseResponse(boolean expectSseEimtter) throws Exception {
		ResponseBodyEmitter emitter = handleValue(Flux.empty(), Flux.class);
		assertEquals(expectSseEimtter, emitter instanceof SseEmitter);
		resetRequest();
	}

	@Test
	public void writeServerSentEvents() throws Exception {

		this.servletRequest.addHeader("Accept", "text/event-stream");
		EmitterProcessor<String> processor = EmitterProcessor.create();
		SseEmitter sseEmitter = (SseEmitter) handleValue(processor, Flux.class);

		EmitterHandler emitterHandler = new EmitterHandler();
		sseEmitter.initialize(emitterHandler);

		processor.onNext("foo");
		processor.onNext("bar");
		processor.onNext("baz");
		processor.onComplete();

		assertEquals("data:foo\n\ndata:bar\n\ndata:baz\n\n", emitterHandler.getOutput());
	}

	@Test
	public void writeSentEventsWithBuilder() throws Exception {

		ResolvableType type = ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class);

		EmitterProcessor<ServerSentEvent<?>> processor = EmitterProcessor.create();
		SseEmitter sseEmitter = (SseEmitter) handleValue(processor, Flux.class, type);

		EmitterHandler emitterHandler = new EmitterHandler();
		sseEmitter.initialize(emitterHandler);

		processor.onNext(ServerSentEvent.builder("foo").id("1").build());
		processor.onNext(ServerSentEvent.builder("bar").id("2").build());
		processor.onNext(ServerSentEvent.builder("baz").id("3").build());
		processor.onComplete();

		assertEquals("id:1\ndata:foo\n\nid:2\ndata:bar\n\nid:3\ndata:baz\n\n",
				emitterHandler.getOutput());
	}

	@Test
	public void writeStreamJson() throws Exception {

		this.servletRequest.addHeader("Accept", "application/stream+json");

		EmitterProcessor<String> processor = EmitterProcessor.create();
		ResponseBodyEmitter emitter = handleValue(processor, Flux.class);

		EmitterHandler emitterHandler = new EmitterHandler();
		emitter.initialize(emitterHandler);

		ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
		emitter.extendResponse(message);

		processor.onNext("[\"foo\",\"bar\"]");
		processor.onNext("[\"bar\",\"baz\"]");
		processor.onComplete();

		assertEquals("application/stream+json", message.getHeaders().getContentType().toString());
		assertEquals("[\"foo\",\"bar\"]\n[\"bar\",\"baz\"]\n", emitterHandler.getOutput());
	}

	@Test
	public void writeText() throws Exception {

		EmitterProcessor<String> processor = EmitterProcessor.create();
		ResponseBodyEmitter emitter = handleValue(processor, Flux.class);

		EmitterHandler emitterHandler = new EmitterHandler();
		emitter.initialize(emitterHandler);

		processor.onNext("The quick");
		processor.onNext(" brown fox jumps over ");
		processor.onNext("the lazy dog");
		processor.onComplete();

		assertEquals("The quick brown fox jumps over the lazy dog", emitterHandler.getOutput());
	}

	@Test
	public void writeTextContentType() throws Exception {

		// Any requested, concrete, "text" media type
		this.servletRequest.addHeader("Accept", "*/*, text/*, text/markdown");
		testEmitterContentType("text/markdown");

		// Or any requested concrete media type
		this.servletRequest.addHeader("Accept", "*/*, text/*, foo/bar");
		testEmitterContentType("foo/bar");

		// Or default to...
		testEmitterContentType("text/plain");

		// Or default to if not concrete..
		this.servletRequest.addHeader("Accept", "text/*");
		testEmitterContentType("text/plain");
	}

	private void testEmitterContentType(String expected) throws Exception {
		ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
		ResponseBodyEmitter emitter = handleValue(Flux.empty(), Flux.class);
		emitter.extendResponse(message);
		assertEquals(expected, message.getHeaders().getContentType().toString());
		resetRequest();
	}


	private void testDeferredResultSubscriber(Object returnValue, Class<?> asyncType,
			Runnable produceTask, Object expected) throws Exception {

		ResponseBodyEmitter emitter = handleValue(returnValue, asyncType);
		assertNull(emitter);

		assertTrue(this.servletRequest.isAsyncStarted());
		assertFalse(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());

		produceTask.run();

		assertTrue(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult());
		assertEquals(expected, WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult());

		resetRequest();
	}

	private ResponseBodyEmitter handleValue(Object returnValue, Class<?> asyncType) throws Exception {
		return handleValue(returnValue, asyncType, ResolvableType.forClass(String.class));
	}

	private ResponseBodyEmitter handleValue(Object returnValue, Class<?> asyncType,
			ResolvableType genericType) throws Exception {

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		MethodParameter returnType = on(TestController.class).resolveReturnType(asyncType, genericType);
		return this.handler.handleValue(returnValue, returnType, mavContainer, this.webRequest);
	}


	@SuppressWarnings("unused")
	static class TestController {

		String handleString() { return null; }

		Mono<String> handleMono() { return null; }

		Single<String> handleSingle() { return null; }

		io.reactivex.Single<String> handleSingleRxJava2() { return null; }

		Flux<String> handleFlux() { return null; }

		Flux<ServerSentEvent<String>> handleFluxSseEventBuilder() { return null; }
	}


	private static class EmitterHandler implements ResponseBodyEmitter.Handler {

		private final StringBuilder stringBuilder = new StringBuilder();


		public String getOutput() {
			return this.stringBuilder.toString();
		}

		@Override
		public void send(Object data, MediaType mediaType) throws IOException {
			this.stringBuilder.append(data);
		}

		@Override
		public void complete() {
		}

		@Override
		public void completeWithError(Throwable failure) {
		}

		@Override
		public void onTimeout(Runnable callback) {
		}

		@Override
		public void onCompletion(Runnable callback) {
		}
	}

}
