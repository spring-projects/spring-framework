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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.ReactorContextAccessor;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestAttributesThreadLocalAccessor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * Tests for {@link ReactiveTypeHandler}.
 *
 * @author Rossen Stoyanchev
 */
class ReactiveTypeHandlerTests {

	private ReactiveTypeHandler handler;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private NativeWebRequest webRequest;


	@BeforeEach
	void setup() throws Exception {
		this.handler = initHandler(new SyncTaskExecutor(), null);
		resetRequest();
	}

	private static ReactiveTypeHandler initHandler(
			TaskExecutor taskExecutor, @Nullable ContextSnapshotFactory snapshotFactory) {

		ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
		factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = factoryBean.getObject();
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		return new ReactiveTypeHandler(adapterRegistry, taskExecutor, manager, snapshotFactory);
	}

	private void resetRequest() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletResponse = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.servletRequest, this.servletResponse);

		AsyncWebRequest webRequest = new StandardServletAsyncWebRequest(this.servletRequest, this.servletResponse);
		WebAsyncUtils.getAsyncManager(this.webRequest).setAsyncWebRequest(webRequest);
		this.servletRequest.setAsyncSupported(true);
	}


	@Test
	void supportsType() {
		assertThat(this.handler.isReactiveType(Mono.class)).isTrue();
		assertThat(this.handler.isReactiveType(Single.class)).isTrue();
	}

	@Test
	void doesNotSupportType() {
		assertThat(this.handler.isReactiveType(String.class)).isFalse();
	}

	@Test
	void findsConcreteStreamingMediaType() {
		final List<MediaType> accept = List.of(
				MediaType.ALL,
				MediaType.parseMediaType("application/*+x-ndjson"),
				MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"));

		assertThat(ReactiveTypeHandler.findConcreteJsonStreamMediaType(accept))
				.isEqualTo(MediaType.APPLICATION_NDJSON);
	}

	@Test
	void findsConcreteStreamingMediaType_vendorFirst() {
		final List<MediaType> accept = List.of(
				MediaType.ALL,
				MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"),
				MediaType.parseMediaType("application/*+x-ndjson"),
				MediaType.APPLICATION_NDJSON);

		assertThat(ReactiveTypeHandler.findConcreteJsonStreamMediaType(accept))
				.hasToString("application/vnd.myapp.v1+x-ndjson");
	}

	@Test
	void findsConcreteStreamingMediaType_plainNdJsonFirst() {
		final List<MediaType> accept = List.of(
				MediaType.ALL,
				MediaType.APPLICATION_NDJSON,
				MediaType.parseMediaType("application/*+x-ndjson"),
				MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"));

		assertThat(ReactiveTypeHandler.findConcreteJsonStreamMediaType(accept))
				.isEqualTo(MediaType.APPLICATION_NDJSON);
	}

	@SuppressWarnings("deprecation")
	@Test
	void findsConcreteStreamingMediaType_plainStreamingJsonFirst() {
		final List<MediaType> accept = List.of(
				MediaType.ALL,
				MediaType.APPLICATION_STREAM_JSON,
				MediaType.parseMediaType("application/*+x-ndjson"),
				MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"));

		assertThat(ReactiveTypeHandler.findConcreteJsonStreamMediaType(accept))
				.isEqualTo(MediaType.APPLICATION_STREAM_JSON);
	}

	@Test
	void deferredResultSubscriberWithOneValue() throws Exception {

		// Mono
		Sinks.One<String> sink = Sinks.one();
		testDeferredResultSubscriber(
				sink.asMono(), Mono.class, forClass(String.class),
				() -> sink.emitValue("foo", Sinks.EmitFailureHandler.FAIL_FAST),
				"foo");

		// Mono empty
		Sinks.One<String> emptySink = Sinks.one();
		testDeferredResultSubscriber(
				emptySink.asMono(), Mono.class, forClass(String.class),
				() -> emptySink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST),
				null);

		// RxJava Single
		AtomicReference<SingleEmitter<String>> ref2 = new AtomicReference<>();
		Single<String> single2 = Single.create(ref2::set);
		testDeferredResultSubscriber(single2, Single.class, forClass(String.class),
				() -> ref2.get().onSuccess("foo"), "foo");
	}

	@Test
	void deferredResultSubscriberWithNoValues() throws Exception {
		Sinks.One<String> sink = Sinks.one();
		testDeferredResultSubscriber(sink.asMono(), Mono.class, forClass(String.class),
				() -> sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST),
				null);
	}

	@Test
	void deferredResultSubscriberWithMultipleValues() throws Exception {

		// JSON must be preferred for Flux<String> -> List<String> or else we stream
		this.servletRequest.addHeader("Accept", "application/json");

		Bar bar1 = new Bar("foo");
		Bar bar2 = new Bar("bar");

		Sinks.Many<Bar> sink = Sinks.many().unicast().onBackpressureBuffer();
		testDeferredResultSubscriber(sink.asFlux(), Flux.class, forClass(Bar.class), () -> {
			sink.tryEmitNext(bar1);
			sink.tryEmitNext(bar2);
			sink.tryEmitComplete();
		}, Arrays.asList(bar1, bar2));
	}

	@Test
	void deferredResultSubscriberWithError() throws Exception {

		IllegalStateException ex = new IllegalStateException();

		// Mono
		Sinks.One<String> sink = Sinks.one();
		testDeferredResultSubscriber(sink.asMono(), Mono.class, forClass(String.class),
				() -> sink.emitError(ex, Sinks.EmitFailureHandler.FAIL_FAST), ex);

		// RxJava Single
		AtomicReference<SingleEmitter<String>> ref2 = new AtomicReference<>();
		Single<String> single2 = Single.create(ref2::set);
		testDeferredResultSubscriber(single2, Single.class, forClass(String.class),
				() -> ref2.get().onError(ex), ex);
	}

	@Test
	void mediaTypes() throws Exception {

		// Media type from request
		this.servletRequest.addHeader("Accept", "text/event-stream");
		testSseResponse(true);

		// Media type from "produces" attribute
		Set<MediaType> types = Collections.singleton(MediaType.TEXT_EVENT_STREAM);
		this.servletRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, types);
		testSseResponse(true);

		// No media type preferences
		testSseResponse(false);
	}

	private void testSseResponse(boolean expectSseEmitter) throws Exception {
		ResponseBodyEmitter emitter = handleValue(Flux.empty(), Flux.class, forClass(String.class));
		Object actual = emitter instanceof SseEmitter;
		assertThat(actual).isEqualTo(expectSseEmitter);
		resetRequest();
	}

	@Test
	void writeServerSentEvents() throws Exception {

		this.servletRequest.addHeader("Accept", "text/event-stream");
		Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
		SseEmitter sseEmitter = (SseEmitter) handleValue(sink.asFlux(), Flux.class, forClass(String.class));

		EmitterHandler emitterHandler = new EmitterHandler();
		sseEmitter.initialize(emitterHandler);

		sink.tryEmitNext("foo");
		sink.tryEmitNext("bar");
		sink.tryEmitNext("baz");
		sink.tryEmitComplete();

		assertThat(emitterHandler.getValuesAsText()).isEqualTo("data:foo\n\ndata:bar\n\ndata:baz\n\n");
	}

	@Test
	void writeServerSentEventsWithBuilder() throws Exception {

		ResolvableType type = ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class);

		Sinks.Many<ServerSentEvent<?>> sink = Sinks.many().unicast().onBackpressureBuffer();
		SseEmitter sseEmitter = (SseEmitter) handleValue(sink.asFlux(), Flux.class, type);

		EmitterHandler emitterHandler = new EmitterHandler();
		sseEmitter.initialize(emitterHandler);

		sink.tryEmitNext(ServerSentEvent.builder("foo").id("1").build());
		sink.tryEmitNext(ServerSentEvent.builder("bar").id("2").build());
		sink.tryEmitNext(ServerSentEvent.builder("baz").id("3").build());
		sink.tryEmitComplete();

		assertThat(emitterHandler.getValuesAsText()).isEqualTo("id:1\ndata:foo\n\nid:2\ndata:bar\n\nid:3\ndata:baz\n\n");
	}

	@Test
	void writeStreamJson() throws Exception {

		this.servletRequest.addHeader("Accept", "application/x-ndjson");

		Sinks.Many<Bar> sink = Sinks.many().unicast().onBackpressureBuffer();
		ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(Bar.class));

		EmitterHandler emitterHandler = new EmitterHandler();
		emitter.initialize(emitterHandler);

		ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
		emitter.extendResponse(message);

		Bar bar1 = new Bar("foo");
		Bar bar2 = new Bar("bar");

		sink.tryEmitNext(bar1);
		sink.tryEmitNext(bar2);
		sink.tryEmitComplete();

		assertThat(message.getHeaders().getContentType()).hasToString("application/x-ndjson");
		assertThat(emitterHandler.getValues()).isEqualTo(Arrays.asList(bar1, "\n", bar2, "\n"));
	}

	@Test
	void writeStreamJsonWithVendorSubtype() throws Exception {
		this.servletRequest.addHeader("Accept", "application/vnd.myapp.v1+x-ndjson");

		Sinks.Many<Bar> sink = Sinks.many().unicast().onBackpressureBuffer();
		ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(Bar.class));

		assertThat(emitter).as("emitter").isNotNull();

		EmitterHandler emitterHandler = new EmitterHandler();
		emitter.initialize(emitterHandler);

		ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
		emitter.extendResponse(message);

		Bar bar1 = new Bar("foo");
		Bar bar2 = new Bar("bar");

		sink.tryEmitNext(bar1);
		sink.tryEmitNext(bar2);
		sink.tryEmitComplete();

		assertThat(message.getHeaders().getContentType()).hasToString("application/vnd.myapp.v1+x-ndjson");
		assertThat(emitterHandler.getValues()).isEqualTo(Arrays.asList(bar1, "\n", bar2, "\n"));
	}

	@Test
	void writeStreamJsonWithWildcardSubtype() throws Exception {
		this.servletRequest.addHeader("Accept", "application/*+x-ndjson");

		Sinks.Many<Bar> sink = Sinks.many().unicast().onBackpressureBuffer();
		ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(Bar.class));

		assertThat(emitter).as("emitter").isNotNull();

		EmitterHandler emitterHandler = new EmitterHandler();
		emitter.initialize(emitterHandler);

		ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
		emitter.extendResponse(message);

		Bar bar1 = new Bar("foo");
		Bar bar2 = new Bar("bar");

		sink.tryEmitNext(bar1);
		sink.tryEmitNext(bar2);
		sink.tryEmitComplete();

		assertThat(message.getHeaders().getContentType()).hasToString("application/x-ndjson");
		assertThat(emitterHandler.getValues()).isEqualTo(Arrays.asList(bar1, "\n", bar2, "\n"));
	}

	@Test
	void writeText() throws Exception {

		Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
		ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(String.class));

		EmitterHandler emitterHandler = new EmitterHandler();
		emitter.initialize(emitterHandler);

		sink.tryEmitNext("The quick");
		sink.tryEmitNext(" brown fox jumps over ");
		sink.tryEmitNext("the lazy dog");
		sink.tryEmitComplete();

		assertThat(emitterHandler.getValuesAsText()).isEqualTo("The quick brown fox jumps over the lazy dog");
	}

	@Test
	void failOnWriteShouldCompleteEmitter() throws Exception {

		Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
		ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(String.class));

		ErroringEmitterHandler emitterHandler = new ErroringEmitterHandler();
		emitter.initialize(emitterHandler);

		sink.tryEmitNext("The quick");
		sink.tryEmitNext(" brown fox jumps over ");
		sink.tryEmitNext("the lazy dog");
		sink.tryEmitComplete();

		assertThat(emitterHandler.getHandlingStatus()).isEqualTo(HandlingStatus.ERROR);
		assertThat(emitterHandler.getFailure()).isInstanceOf(IOException.class);
	}

	@Test
	void writeFluxOfString() throws Exception {

		// Default to "text/plain"
		testEmitterContentType("text/plain");

		// Same if no concrete media type
		this.servletRequest.addHeader("Accept", "text/*");
		testEmitterContentType("text/plain");

		// Otherwise pick concrete media type
		this.servletRequest.addHeader("Accept", "*/*, text/*, text/markdown");
		testEmitterContentType("text/markdown");

		// Any concrete media type
		this.servletRequest.addHeader("Accept", "*/*, text/*, foo/bar");
		testEmitterContentType("foo/bar");

		// Including json
		this.servletRequest.addHeader("Accept", "*/*, text/*, application/json");
		testEmitterContentType("application/json");
	}

	@Test
	void contextPropagation() throws Exception {

		ContextRegistry registry = new ContextRegistry();
		registry.registerThreadLocalAccessor(new RequestAttributesThreadLocalAccessor());
		registry.registerContextAccessor(new ReactorContextAccessor());
		ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().contextRegistry(registry).build();

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		MethodParameter returnType = on(TestController.class).resolveReturnType(Flux.class, forClass(String.class));
		ReactiveTypeHandler handler = initHandler(new SimpleAsyncTaskExecutor(), snapshotFactory);

		this.servletRequest.addHeader("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);
		this.servletRequest.setAttribute("key", "context value");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(this.servletRequest));

		try {
			Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
			ResponseBodyEmitter emitter = handler.handleValue(sink.asFlux(), returnType, mavContainer, this.webRequest);

			ContextEmitterHandler emitterHandler = new ContextEmitterHandler();
			emitter.initialize(emitterHandler);

			sink.tryEmitNext("emitted value");
			emitterHandler.awaitMessageCount(1);

			sink.tryEmitComplete();

			assertThat(emitterHandler.getValuesAsText()).isEqualTo("data:emitted value\n\n");
			assertThat(emitterHandler.getSavedRequest()).isSameAs(this.servletRequest);
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
		}
	}

	private void testEmitterContentType(String expected) throws Exception {
		ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
		ResponseBodyEmitter emitter = handleValue(Flux.empty(), Flux.class, forClass(String.class));
		emitter.extendResponse(message);
		assertThat(message.getHeaders().getContentType().toString()).isEqualTo(expected);
		resetRequest();
	}


	private void testDeferredResultSubscriber(Object returnValue, Class<?> asyncType,
			ResolvableType elementType, Runnable produceTask, Object expected) throws Exception {

		ResponseBodyEmitter emitter = handleValue(returnValue, asyncType, elementType);
		assertThat(emitter).isNull();

		assertThat(this.servletRequest.isAsyncStarted()).isTrue();
		assertThat(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult()).isFalse();

		produceTask.run();

		assertThat(WebAsyncUtils.getAsyncManager(this.webRequest).hasConcurrentResult()).isTrue();
		assertThat(WebAsyncUtils.getAsyncManager(this.webRequest).getConcurrentResult()).isEqualTo(expected);

		resetRequest();
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

		Flux<Bar> handleFlux() { return null; }

		Flux<String> handleFluxString() { return null; }

		Flux<ServerSentEvent<String>> handleFluxSseEventBuilder() { return null; }
	}


	private static class EmitterHandler implements ResponseBodyEmitter.Handler {

		private final List<Object> values = new ArrayList<>();

		private HandlingStatus handlingStatus;

		private Throwable failure;


		public List<?> getValues() {
			return this.values;
		}

		public String getValuesAsText() {
			return this.values.stream().map(Object::toString).collect(Collectors.joining());
		}

		public HandlingStatus getHandlingStatus() {
			return this.handlingStatus;
		}

		public Throwable getFailure() {
			return this.failure;
		}

		@Override
		public void send(Object data, MediaType mediaType) throws IOException {
			this.values.add(data);
		}

		@Override
		public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
			items.forEach(item -> this.values.add(item.getData()));
		}

		@Override
		public void complete() {
			this.handlingStatus = HandlingStatus.SUCCESS;
		}

		@Override
		public void completeWithError(Throwable failure) {
			this.handlingStatus = HandlingStatus.ERROR;
			this.failure = failure;
		}

		@Override
		public void onTimeout(Runnable callback) {
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
		}

		@Override
		public void onCompletion(Runnable callback) {
		}
	}

	private enum HandlingStatus {
		SUCCESS,ERROR
	}

	private static class ErroringEmitterHandler extends EmitterHandler {
		@Override
		public void send(Object data, MediaType mediaType) throws IOException {
			throw new IOException();
		}

		@Override
		public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
			throw new IOException();
		}
	}


	private static class ContextEmitterHandler extends EmitterHandler {

		private final AtomicInteger count = new AtomicInteger();

		private HttpServletRequest savedRequest;

		public HttpServletRequest getSavedRequest() {
			return this.savedRequest;
		}

		@Override
		public void send(Object data, MediaType mediaType) throws IOException {
			saveRequest();
			super.send(data, mediaType);
			this.count.addAndGet(1);
		}

		@Override
		public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
			saveRequest();
			for (ResponseBodyEmitter.DataWithMediaType item : items) {
				super.send(item.getData(), item.getMediaType());
			}
			this.count.addAndGet(1);
		}

		private void saveRequest() {
			RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
			this.savedRequest = ((ServletRequestAttributes) attributes).getRequest();
		}

		public void awaitMessageCount(int count) throws InterruptedException {
			for (int i = 0; i < 10 && this.count.get() < count; i++) {
				Thread.sleep(10);
			}
			assertThat(this.count.get()).isGreaterThanOrEqualTo(count);
		}
	}


	private static class Bar {

		private final String value;

		public Bar(String value) {
			this.value = value;
		}

		@SuppressWarnings("unused")
		public String getValue() {
			return this.value;
		}
	}

}
