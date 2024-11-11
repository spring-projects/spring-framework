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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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

	private static final long STREAMING_TIMEOUT_VALUE = -1;

	private static final MediaType WILDCARD_SUBTYPE_SUFFIXED_BY_NDJSON = MediaType.valueOf("application/*+x-ndjson");

	private static final boolean isContextPropagationPresent = ClassUtils.isPresent(
			"io.micrometer.context.ContextSnapshot", ReactiveTypeHandler.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(ReactiveTypeHandler.class);


	private final ReactiveAdapterRegistry adapterRegistry;

	private final TaskExecutor taskExecutor;

	private final ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private final Object contextSnapshotHelper;


	public ReactiveTypeHandler() {
		this(ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(), new ContentNegotiationManager(), null);
	}

	ReactiveTypeHandler(
			ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager,
			@Nullable Object contextSnapshotFactory) {

		Assert.notNull(registry, "ReactiveAdapterRegistry is required");
		Assert.notNull(executor, "TaskExecutor is required");
		Assert.notNull(manager, "ContentNegotiationManager is required");
		this.adapterRegistry = registry;
		this.taskExecutor = executor;
		this.contentNegotiationManager = manager;
		this.contextSnapshotHelper = initContextSnapshotHelper(contextSnapshotFactory);
	}

	@Nullable
	private static Object initContextSnapshotHelper(@Nullable Object snapshotFactory) {
		if (isContextPropagationPresent) {
			return new ContextSnapshotHelper((ContextSnapshotFactory) snapshotFactory);
		}
		return null;
	}


	/**
	 * Whether the type can be adapted to a Reactive Streams {@link Publisher}.
	 */
	public boolean isReactiveType(Class<?> type) {
		return (this.adapterRegistry.getAdapter(type) != null);
	}


	/**
	 * Process the given reactive return value and decide whether to adapt it
	 * to a {@link ResponseBodyEmitter} or a {@link DeferredResult}.
	 * @return an emitter for streaming, or {@code null} if handled internally
	 * with a {@link DeferredResult}
	 */
	@Nullable
	public ResponseBodyEmitter handleValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mav, NativeWebRequest request) throws Exception {

		Assert.notNull(returnValue, "Expected return value");
		Class<?> clazz = returnValue.getClass();
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(clazz);
		Assert.state(adapter != null, () -> "Unexpected return value type: " + clazz);

		TaskDecorator taskDecorator = null;
		if (isContextPropagationPresent) {
			ContextSnapshotHelper helper = (ContextSnapshotHelper) this.contextSnapshotHelper;
			Assert.notNull(helper, "No ContextSnapshotHelper");
			returnValue = helper.writeReactorContext(returnValue);
			taskDecorator = helper.getTaskDecorator();
		}

		ResolvableType elementType = ResolvableType.forMethodParameter(returnType).getGeneric();
		Class<?> elementClass = elementType.toClass();

		Collection<MediaType> mediaTypes = getMediaTypes(request);
		Optional<MediaType> mediaType = mediaTypes.stream().filter(MimeType::isConcrete).findFirst();

		if (adapter.isMultiValue()) {
			if (mediaTypes.stream().anyMatch(MediaType.TEXT_EVENT_STREAM::includes) ||
					ServerSentEvent.class.isAssignableFrom(elementClass)) {
				SseEmitter emitter = new SseEmitter(STREAMING_TIMEOUT_VALUE);
				new SseEmitterSubscriber(emitter, this.taskExecutor, taskDecorator).connect(adapter, returnValue);
				return emitter;
			}
			if (CharSequence.class.isAssignableFrom(elementClass)) {
				ResponseBodyEmitter emitter = getEmitter(mediaType.orElse(MediaType.TEXT_PLAIN));
				new TextEmitterSubscriber(emitter, this.taskExecutor).connect(adapter, returnValue);
				return emitter;
			}
			MediaType streamingResponseType = findConcreteJsonStreamMediaType(mediaTypes);
			if (streamingResponseType != null) {
				ResponseBodyEmitter emitter = getEmitter(streamingResponseType);
				new JsonEmitterSubscriber(emitter, this.taskExecutor).connect(adapter, returnValue);
				return emitter;
			}
		}

		// Not streaming...
		DeferredResult<Object> result = new DeferredResult<>();
		new DeferredResultSubscriber(result, adapter, elementType).connect(adapter, returnValue);
		WebAsyncUtils.getAsyncManager(request).startDeferredResultProcessing(result, mav);

		return null;
	}

	/**
	 * Attempts to find a concrete {@code MediaType} that can be streamed (as json separated
	 * by newlines in the response body). This method considers two concrete types
	 * {@code APPLICATION_NDJSON} and {@code APPLICATION_STREAM_JSON}) as well as any
	 * subtype of application that has the {@code +x-ndjson} suffix. In the later case,
	 * the media type MUST be concrete for it to be considered.
	 *
	 * <p>For example {@code application/vnd.myapp+x-ndjson} is considered a streaming type
	 * while {@code application/*+x-ndjson} isn't.
	 * @param acceptedMediaTypes the collection of acceptable media types in the request
	 * @return the concrete streaming {@code MediaType} if one could be found or {@code null}
	 * if none could be found
	 */
	@SuppressWarnings("deprecation")
	@Nullable
	static MediaType findConcreteJsonStreamMediaType(Collection<MediaType> acceptedMediaTypes) {
		for (MediaType acceptedType : acceptedMediaTypes) {
			if (WILDCARD_SUBTYPE_SUFFIXED_BY_NDJSON.includes(acceptedType)) {
				if (acceptedType.isConcrete()) {
					return acceptedType;
				}
				else {
					// if not concrete, it must be application/*+x-ndjson: we assume
					// that the requester is only interested in the ndjson nature of
					// the underlying representation and can parse any example of that
					// underlying representation, so we use the ndjson media type.
					return MediaType.APPLICATION_NDJSON;
				}
			}
			else if (MediaType.APPLICATION_NDJSON.includes(acceptedType)) {
				return MediaType.APPLICATION_NDJSON;
			}
			else if (MediaType.APPLICATION_STREAM_JSON.includes(acceptedType)) {
				return MediaType.APPLICATION_STREAM_JSON;
			}
		}
		return null; // not a concrete streaming type
	}

	@SuppressWarnings("unchecked")
	private Collection<MediaType> getMediaTypes(NativeWebRequest request)
			throws HttpMediaTypeNotAcceptableException {

		Collection<MediaType> mediaTypes = (Collection<MediaType>) request.getAttribute(
				HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		return CollectionUtils.isEmpty(mediaTypes) ?
				this.contentNegotiationManager.resolveMediaTypes(request) : mediaTypes;
	}

	private ResponseBodyEmitter getEmitter(MediaType mediaType) {
		return new ResponseBodyEmitter(STREAMING_TIMEOUT_VALUE) {
			@Override
			protected void extendResponse(ServerHttpResponse outputMessage) {
				outputMessage.getHeaders().setContentType(mediaType);
			}
		};
	}


	private abstract static class AbstractEmitterSubscriber implements Subscriber<Object>, Runnable {

		private final ResponseBodyEmitter emitter;

		private final TaskExecutor taskExecutor;

		@Nullable
		private Subscription subscription;

		private final AtomicReference<Object> elementRef = new AtomicReference<>();

		@Nullable
		private Throwable error;

		private volatile boolean terminated;

		private final AtomicLong executing = new AtomicLong();

		private volatile boolean done;

		private final Runnable sendTask;

		protected AbstractEmitterSubscriber(
				ResponseBodyEmitter emitter, TaskExecutor executor, @Nullable TaskDecorator taskDecorator) {

			this.emitter = emitter;
			this.taskExecutor = executor;
			this.sendTask = (taskDecorator != null ? taskDecorator.decorate(this) : this);
		}

		public void connect(ReactiveAdapter adapter, Object returnValue) {
			Publisher<Object> publisher = adapter.toPublisher(returnValue);
			publisher.subscribe(this);
		}

		protected ResponseBodyEmitter getEmitter() {
			return this.emitter;
		}

		@Override
		public final void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			this.emitter.onTimeout(() -> {
				if (logger.isTraceEnabled()) {
					logger.trace("Connection timeout for " + this.emitter);
				}
				terminate();
				this.emitter.complete();
			});
			this.emitter.onError(this.emitter::completeWithError);
			subscription.request(1);
		}

		@Override
		public final void onNext(Object element) {
			this.elementRef.lazySet(element);
			trySchedule();
		}

		@Override
		public final void onError(Throwable ex) {
			this.error = ex;
			this.terminated = true;
			trySchedule();
		}

		@Override
		public final void onComplete() {
			this.terminated = true;
			trySchedule();
		}

		private void trySchedule() {
			if (this.executing.getAndIncrement() == 0) {
				schedule();
			}
		}

		private void schedule() {
			try {
				this.taskExecutor.execute(this.sendTask);
			}
			catch (Throwable ex) {
				try {
					terminate();
				}
				finally {
					this.executing.decrementAndGet();
					this.elementRef.lazySet(null);
				}
			}
		}

		@Override
		public void run() {
			if (this.done) {
				this.elementRef.lazySet(null);
				return;
			}

			// Check terminal signal before processing element..
			boolean isTerminated = this.terminated;

			Object element = this.elementRef.get();
			if (element != null) {
				this.elementRef.lazySet(null);
				Assert.state(this.subscription != null, "No subscription");
				try {
					send(element);
					this.subscription.request(1);
				}
				catch (final Throwable ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("Send for " + this.emitter + " failed: " + ex);
					}
					terminate();
					this.emitter.completeWithError(ex);
					return;
				}
			}

			if (isTerminated) {
				this.done = true;
				Throwable ex = this.error;
				this.error = null;
				if (ex != null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Publisher for " + this.emitter + " failed: " + ex);
					}
					this.emitter.completeWithError(ex);
				}
				else {
					if (logger.isTraceEnabled()) {
						logger.trace("Publisher for " + this.emitter + " completed");
					}
					this.emitter.complete();
				}
				return;
			}

			if (this.executing.decrementAndGet() != 0) {
				schedule();
			}
		}

		protected abstract void send(Object element) throws IOException;

		private void terminate() {
			this.done = true;
			if (this.subscription != null) {
				this.subscription.cancel();
			}
		}
	}


	private static class SseEmitterSubscriber extends AbstractEmitterSubscriber {

		SseEmitterSubscriber(SseEmitter sseEmitter, TaskExecutor executor, @Nullable TaskDecorator taskDecorator) {
			super(sseEmitter, executor, taskDecorator);
		}

		@Override
		protected void send(Object element) throws IOException {
			if (element instanceof ServerSentEvent<?> event) {
				((SseEmitter) getEmitter()).send(adapt(event));
			}
			else {
				getEmitter().send(element, MediaType.APPLICATION_JSON);
			}
		}

		private SseEmitter.SseEventBuilder adapt(ServerSentEvent<?> sse) {
			SseEmitter.SseEventBuilder builder = SseEmitter.event();
			String id = sse.id();
			String event = sse.event();
			Duration retry = sse.retry();
			String comment = sse.comment();
			Object data = sse.data();
			if (id != null) {
				builder.id(id);
			}
			if (event != null) {
				builder.name(event);
			}
			if (data != null) {
				builder.data(data);
			}
			if (retry != null) {
				builder.reconnectTime(retry.toMillis());
			}
			if (comment != null) {
				builder.comment(comment);
			}
			return builder;
		}
	}


	private static class JsonEmitterSubscriber extends AbstractEmitterSubscriber {

		JsonEmitterSubscriber(
				ResponseBodyEmitter emitter, TaskExecutor executor) {

			super(emitter, executor, null);
		}

		@Override
		protected void send(Object element) throws IOException {
			getEmitter().send(element, MediaType.APPLICATION_JSON);
			getEmitter().send("\n", MediaType.TEXT_PLAIN);
		}
	}


	private static class TextEmitterSubscriber extends AbstractEmitterSubscriber {

		TextEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
			super(emitter, executor, null);
		}

		@Override
		protected void send(Object element) throws IOException {
			getEmitter().send(element, MediaType.TEXT_PLAIN);
		}
	}


	private static class DeferredResultSubscriber implements Subscriber<Object> {

		private final DeferredResult<Object> result;

		private final boolean multiValueSource;

		private final CollectedValuesList values;

		DeferredResultSubscriber(DeferredResult<Object> result, ReactiveAdapter adapter, ResolvableType elementType) {
			this.result = result;
			this.multiValueSource = adapter.isMultiValue();
			this.values = new CollectedValuesList(elementType);
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
			if (this.values.size() > 1 || this.multiValueSource) {
				this.result.setResult(this.values);
			}
			else if (this.values.size() == 1) {
				this.result.setResult(this.values.get(0));
			}
			else {
				this.result.setResult(null);
			}
		}
	}


	/**
	 * List of collect values where all elements are a specified type.
	 */
	@SuppressWarnings("serial")
	static class CollectedValuesList extends ArrayList<Object> {

		private final ResolvableType elementType;

		CollectedValuesList(ResolvableType elementType) {
			this.elementType = elementType;
		}

		public ResolvableType getReturnType() {
			return ResolvableType.forClassWithGenerics(List.class, this.elementType);
		}
	}


	private static final class ContextSnapshotHelper {

		private final ContextSnapshotFactory snapshotFactory;

		private ContextSnapshotHelper(@Nullable ContextSnapshotFactory factory) {
			this.snapshotFactory = (factory != null ? factory : ContextSnapshotFactory.builder().build());
		}

		@SuppressWarnings("ReactiveStreamsUnusedPublisher")
		public Object writeReactorContext(Object returnValue) {
			if (Mono.class.isAssignableFrom(returnValue.getClass())) {
				ContextSnapshot snapshot = this.snapshotFactory.captureAll();
				return ((Mono<?>) returnValue).contextWrite(snapshot::updateContext);
			}
			else if (Flux.class.isAssignableFrom(returnValue.getClass())) {
				ContextSnapshot snapshot = this.snapshotFactory.captureAll();
				return ((Flux<?>) returnValue).contextWrite(snapshot::updateContext);
			}
			else {
				return returnValue;
			}
		}

		public TaskDecorator getTaskDecorator() {
			return new ContextPropagatingTaskDecorator(this.snapshotFactory);
		}
	}

}
