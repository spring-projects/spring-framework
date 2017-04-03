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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;


/**
 * Private helper class to assist with handling "reactive" return values types
 * that can be adapted to a Reactive Streams {@link Publisher} through the
 * {@link ReactiveAdapterRegistry}.
 *
 * <p>Such return values may be bridged to a {@link ResponseBodyEmitter} for
 * streaming purposes at the presence of a streaming media type or based on the
 * generic type.
 *
 * <p>For all other cases {@code Publisher} output is collected and bridged to
 * {@link DeferredResult} for standard async request processing.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ReactiveTypeHandler {

	private static final MediaType JSON_TYPE = new MediaType("application", "*+json");


	private final ReactiveAdapterRegistry reactiveRegistry;

	private final ContentNegotiationManager contentNegotiationManager;


	ReactiveTypeHandler(ReactiveAdapterRegistry registry, ContentNegotiationManager manager) {
		Assert.notNull(registry, "ReactiveAdapterRegistry is required");
		Assert.notNull(manager, "ContentNegotiationManager is required");
		this.reactiveRegistry = registry;
		this.contentNegotiationManager = manager;
	}


	/**
	 * Whether the type can be adapted to a Reactive Streams {@link Publisher}.
	 */
	public boolean isReactiveType(Class<?> type) {
		return this.reactiveRegistry.hasAdapters() && this.reactiveRegistry.getAdapter(type) != null;
	}


	/**
	 * Process the given reactive return value and decide whether to adapt it
	 * to a {@link ResponseBodyEmitter} or a {@link DeferredResult}.
	 *
	 * @return an emitter for streaming or {@code null} if handled internally
	 * with a {@link DeferredResult}.
	 */
	public ResponseBodyEmitter handleValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mav, NativeWebRequest request) throws Exception {

		Assert.notNull(returnValue, "Expected return value");
		ReactiveAdapter adapter = this.reactiveRegistry.getAdapter(returnValue.getClass());
		Assert.state(adapter != null, "Unexpected return value: " + returnValue);

		Class<?> elementType = returnType.nested().getNestedParameterType();
		
		Collection<MediaType> mediaTypes = getMediaTypes(request);
		Optional<MediaType> mediaType = mediaTypes.stream().filter(MimeType::isConcrete).findFirst();
		boolean jsonArrayOfStrings = isJsonArrayOfStrings(elementType, mediaType);

		if (adapter.isMultiValue()) {
			if (mediaTypes.stream().anyMatch(MediaType.TEXT_EVENT_STREAM::includes) ||
					ServerSentEvent.class.isAssignableFrom(elementType)) {
				SseEmitter emitter = new SseEmitter();
				new SseEmitterSubscriber(emitter).connect(adapter, returnValue);
				return emitter;
			}
			if (mediaTypes.stream().anyMatch(MediaType.APPLICATION_STREAM_JSON::includes)) {
				ResponseBodyEmitter emitter = getEmitter(MediaType.APPLICATION_STREAM_JSON);
				new JsonEmitterSubscriber(emitter).connect(adapter, returnValue);
				return emitter;
			}
			if (CharSequence.class.isAssignableFrom(elementType) && !jsonArrayOfStrings) {
				ResponseBodyEmitter emitter = getEmitter(mediaType.orElse(MediaType.TEXT_PLAIN));
				new TextEmitterSubscriber(emitter).connect(adapter, returnValue);
				return emitter;
			}
		}

		// Not streaming...
		DeferredResult<Object> result = new DeferredResult<>();
		new DeferredResultSubscriber(result, jsonArrayOfStrings).connect(adapter, returnValue);
		WebAsyncUtils.getAsyncManager(request).startDeferredResultProcessing(result, mav);

		return null;
	}

	@SuppressWarnings("unchecked")
	private Collection<MediaType> getMediaTypes(NativeWebRequest request)
			throws HttpMediaTypeNotAcceptableException {

		Collection<MediaType> mediaTypes = (Collection<MediaType>) request.getAttribute(
				HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		return CollectionUtils.isEmpty(mediaTypes) ?
				this.contentNegotiationManager.resolveMediaTypes(request) : mediaTypes;
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private boolean isJsonArrayOfStrings(Class<?> elementType, Optional<MediaType> mediaType) {
		return CharSequence.class.isAssignableFrom(elementType) && mediaType.filter(type ->
				MediaType.APPLICATION_JSON.includes(type) || JSON_TYPE.includes(type)).isPresent();
	}

	private ResponseBodyEmitter getEmitter(MediaType mediaType) {
		return new ResponseBodyEmitter() {

			@Override
			protected void extendResponse(ServerHttpResponse outputMessage) {
				outputMessage.getHeaders().setContentType(mediaType);
			}
		};
	}


	private static abstract class AbstractEmitterSubscriber implements Subscriber<Object> {

		private final ResponseBodyEmitter emitter;

		private Subscription subscription;


		protected AbstractEmitterSubscriber(ResponseBodyEmitter emitter) {
			this.emitter = emitter;
		}


		public void connect(ReactiveAdapter adapter, Object returnValue) {
			Publisher<Object> publisher = adapter.toPublisher(returnValue);
			publisher.subscribe(this);
		}


		protected ResponseBodyEmitter getEmitter() {
			return this.emitter;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			this.emitter.onTimeout(subscription::cancel);
			subscription.request(1);
		}

		@Override
		public void onNext(Object element) {
			try {
				send(element);
				this.subscription.request(1);
			}
			catch (IOException ex) {
				this.subscription.cancel();
			}
		}

		protected abstract void send(Object element) throws IOException;

		@Override
		public void onError(Throwable ex) {
			this.emitter.completeWithError(ex);
		}

		@Override
		public void onComplete() {
			this.emitter.complete();
		}
	}


	private static class SseEmitterSubscriber extends AbstractEmitterSubscriber {

		SseEmitterSubscriber(SseEmitter sseEmitter) {
			super(sseEmitter);
		}

		@Override
		protected void send(Object element) throws IOException {
			if (element instanceof ServerSentEvent) {
				ServerSentEvent<?> event = (ServerSentEvent<?>) element;
				((SseEmitter) getEmitter()).send(adapt(event));
			}
			else {
				getEmitter().send(element, MediaType.APPLICATION_JSON);
			}
		}

		private SseEmitter.SseEventBuilder adapt(ServerSentEvent<?> event) {
			SseEmitter.SseEventBuilder builder = SseEmitter.event();
			event.id().ifPresent(builder::id);
			event.comment().ifPresent(builder::comment);
			event.data().ifPresent(builder::data);
			event.retry().ifPresent(duration -> builder.reconnectTime(duration.toMillis()));
			return builder;
		}
	}


	private static class JsonEmitterSubscriber extends AbstractEmitterSubscriber {

		JsonEmitterSubscriber(ResponseBodyEmitter emitter) {
			super(emitter);
		}

		@Override
		protected void send(Object element) throws IOException {
			getEmitter().send(element, MediaType.APPLICATION_JSON);
			getEmitter().send("\n", MediaType.TEXT_PLAIN);
		}
	}


	private static class TextEmitterSubscriber extends AbstractEmitterSubscriber {

		TextEmitterSubscriber(ResponseBodyEmitter emitter) {
			super(emitter);
		}

		@Override
		protected void send(Object element) throws IOException {
			getEmitter().send(element, MediaType.TEXT_PLAIN);
		}
	}


	private static class DeferredResultSubscriber implements Subscriber<Object> {

		private final DeferredResult<Object> result;

		private final boolean jsonArrayOfStrings;

		private final CollectedValuesList values = new CollectedValuesList();


		DeferredResultSubscriber(DeferredResult<Object> result, boolean jsonArrayOfStrings) {
			this.result = result;
			this.jsonArrayOfStrings = jsonArrayOfStrings;
		}


		public void connect(ReactiveAdapter adapter, Object returnValue) {
			Publisher<Object> publisher = adapter.toPublisher(returnValue);
			publisher.subscribe(this);
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.result.onTimeout(subscription::cancel);
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Object element) {
			this.values.add(element);
		}

		@Override
		public void onError(Throwable ex) {
			this.result.setErrorResult(ex);
		}

		@Override
		public void onComplete() {
			if (this.values.size() > 1) {
				this.result.setResult(this.values);
			}
			else if (this.values.size() == 1) {
				this.result.setResult(this.values.get(0));
			}
			else {
				this.result.setResult(this.jsonArrayOfStrings ? this.values : null);
			}
		}
	}

	@SuppressWarnings("serial")
	static class CollectedValuesList extends ArrayList<Object> {
	}

}