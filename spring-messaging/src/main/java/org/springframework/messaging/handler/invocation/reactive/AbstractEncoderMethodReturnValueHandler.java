/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.handler.invocation.reactive;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Base class for a return value handler that encodes the return value, possibly
 * a {@link Publisher} of values, to a {@code Flux<DataBuffer>} through a
 * compatible {@link Encoder}.
 *
 * <p>Sub-classes must implement the abstract method
 * {@link #handleEncodedContent} to do something with the resulting encoded
 * content.
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


	protected final Log logger = LogFactory.getLog(getClass());


	private final List<Encoder<?>> encoders;

	private final ReactiveAdapterRegistry adapterRegistry;

	// TODO: configure or passed via MessageHeaders
	private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


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
		return true;
	}

	@Override
	public Mono<Void> handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) {
		Flux<DataBuffer> encodedContent = encodeContent(returnValue, returnType, this.bufferFactory);
		return handleEncodedContent(encodedContent, returnType, message);
	}

	@SuppressWarnings("unchecked")
	private Flux<DataBuffer> encodeContent(@Nullable Object content, MethodParameter returnType,
			DataBufferFactory bufferFactory) {

		ResolvableType bodyType = ResolvableType.forMethodParameter(returnType);
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(bodyType.resolve(), content);

		Publisher<?> publisher;
		ResolvableType elementType;
		if (adapter != null) {
			publisher = adapter.toPublisher(content);
			ResolvableType genericType = bodyType.getGeneric();
			elementType = getElementType(adapter, genericType);
		}
		else {
			publisher = Mono.justOrEmpty(content);
			elementType = (bodyType.toClass() == Object.class && content != null ?
					ResolvableType.forInstance(content) : bodyType);
		}

		if (elementType.resolve() == void.class || elementType.resolve() == Void.class) {
			return Flux.from(publisher).cast(DataBuffer.class);
		}

		if (logger.isDebugEnabled()) {
			logger.debug((publisher instanceof Mono ? "0..1" : "0..N") + " [" + elementType + "]");
		}

		for (Encoder<?> encoder : getEncoders()) {
			if (encoder.canEncode(elementType, null)) {
				Map<String, Object> hints = Collections.emptyMap();
				return encoder.encode((Publisher) publisher, bufferFactory, elementType, null, hints);
			}
		}

		return Flux.error(new MessagingException("No encoder for " + returnType));
	}

	private ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType genericType) {
		if (adapter.isNoValue()) {
			return VOID_RESOLVABLE_TYPE;
		}
		else if (genericType != ResolvableType.NONE) {
			return genericType;
		}
		else {
			return OBJECT_RESOLVABLE_TYPE;
		}
	}

	/**
	 * Handle the encoded content in some way, e.g. wrapping it in a message and
	 * passing it on for further processing.
	 * @param encodedContent the result of data encoding
	 * @param returnType return type of the handler method that produced the data
	 * @param message the input message handled by the handler method
	 * @return completion {@code Mono<Void>} for the handling
	 */
	protected abstract Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message);

}
