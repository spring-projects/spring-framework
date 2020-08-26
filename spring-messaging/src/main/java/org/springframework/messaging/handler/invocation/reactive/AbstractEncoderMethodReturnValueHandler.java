/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import kotlin.reflect.KFunction;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Base class for a return value handler that encodes return values to
 * {@code Flux<DataBuffer>} through the configured {@link Encoder}s.
 *
 * <p>Subclasses must implement the abstract method
 * {@link #handleEncodedContent} to handle the resulting encoded content.
 *
 * <p>This handler should be ordered last since its {@link #supportsReturnType}
 * returns {@code true} for any method parameter type.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public abstract class AbstractEncoderMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final ResolvableType VOID_RESOLVABLE_TYPE = ResolvableType.forClass(Void.class);

	private static final ResolvableType OBJECT_RESOLVABLE_TYPE = ResolvableType.forClass(Object.class);

	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";


	protected final Log logger = LogFactory.getLog(getClass());

	private final List<Encoder<?>> encoders;

	private final ReactiveAdapterRegistry adapterRegistry;


	protected AbstractEncoderMethodReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
		Assert.notEmpty(encoders, "At least one Encoder is required");
		Assert.notNull(registry, "ReactiveAdapterRegistry is required");
		this.encoders = Collections.unmodifiableList(encoders);
		this.adapterRegistry = registry;
	}


	/**
	 * The configured encoders.
	 */
	public List<Encoder<?>> getEncoders() {
		return this.encoders;
	}

	/**
	 * The configured adapter registry.
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// We could check canEncode but we're probably last in order anyway
		return true;
	}

	@Override
	public Mono<Void> handleReturnValue(
			@Nullable Object returnValue, MethodParameter returnType, Message<?> message) {

		if (returnValue == null) {
			return handleNoContent(returnType, message);
		}

		DataBufferFactory bufferFactory = (DataBufferFactory) message.getHeaders()
				.getOrDefault(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER,
						DefaultDataBufferFactory.sharedInstance);

		MimeType mimeType = (MimeType) message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		Flux<DataBuffer> encodedContent = encodeContent(
				returnValue, returnType, bufferFactory, mimeType, Collections.emptyMap());

		return new ChannelSendOperator<>(encodedContent, publisher ->
				handleEncodedContent(Flux.from(publisher), returnType, message));
	}

	@SuppressWarnings("unchecked")
	private Flux<DataBuffer> encodeContent(
			@Nullable Object content, MethodParameter returnType, DataBufferFactory bufferFactory,
			@Nullable MimeType mimeType, Map<String, Object> hints) {

		ResolvableType returnValueType = ResolvableType.forMethodParameter(returnType);
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(returnValueType.resolve(), content);

		Publisher<?> publisher;
		ResolvableType elementType;
		if (adapter != null) {
			publisher = adapter.toPublisher(content);
			boolean isUnwrapped = KotlinDetector.isKotlinReflectPresent() &&
					KotlinDetector.isKotlinType(returnType.getContainingClass()) &&
					KotlinDelegate.isSuspend(returnType.getMethod()) &&
					!COROUTINES_FLOW_CLASS_NAME.equals(returnValueType.toClass().getName());
			ResolvableType genericType = isUnwrapped ? returnValueType : returnValueType.getGeneric();
			elementType = getElementType(adapter, genericType);
		}
		else {
			publisher = Mono.justOrEmpty(content);
			elementType = (returnValueType.toClass() == Object.class && content != null ?
					ResolvableType.forInstance(content) : returnValueType);
		}

		if (elementType.resolve() == void.class || elementType.resolve() == Void.class) {
			return Flux.from(publisher).cast(DataBuffer.class);
		}

		Encoder<?> encoder = getEncoder(elementType, mimeType);
		return Flux.from(publisher).map(value ->
				encodeValue(value, elementType, encoder, bufferFactory, mimeType, hints));
	}

	private ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType type) {
		if (adapter.isNoValue()) {
			return VOID_RESOLVABLE_TYPE;
		}
		else if (type != ResolvableType.NONE) {
			return type;
		}
		else {
			return OBJECT_RESOLVABLE_TYPE;
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T> Encoder<T> getEncoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Encoder<?> encoder : getEncoders()) {
			if (encoder.canEncode(elementType, mimeType)) {
				return (Encoder<T>) encoder;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> DataBuffer encodeValue(
			Object element, ResolvableType elementType, @Nullable Encoder<T> encoder,
			DataBufferFactory bufferFactory, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		if (encoder == null) {
			encoder = getEncoder(ResolvableType.forInstance(element), mimeType);
			if (encoder == null) {
				throw new MessagingException(
						"No encoder for " + elementType + ", current value type is " + element.getClass());
			}
		}
		return encoder.encodeValue((T) element, bufferFactory, elementType, mimeType, hints);
	}

	/**
	 * Subclasses implement this method to handle encoded values in some way
	 * such as creating and sending messages.
	 * @param encodedContent the encoded content; each {@code DataBuffer}
	 * represents the fully-aggregated, encoded content for one value
	 * (i.e. payload) returned from the HandlerMethod.
	 * @param returnType return type of the handler method that produced the data
	 * @param message the input message handled by the handler method
	 * @return completion {@code Mono<Void>} for the handling
	 */
	protected abstract Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message);

	/**
	 * Invoked for a {@code null} return value, which could mean a void method
	 * or method returning an async type parameterized by void.
	 * @param returnType return type of the handler method that produced the data
	 * @param message the input message handled by the handler method
	 * @return completion {@code Mono<Void>} for the handling
	 */
	protected abstract Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message);


	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		private static boolean isSuspend(@Nullable Method method) {
			if (method == null) {
				return false;
			}
			KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
			return (function != null && function.isSuspend());
		}
	}

}
