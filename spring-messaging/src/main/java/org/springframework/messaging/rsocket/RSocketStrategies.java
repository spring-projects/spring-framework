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

package org.springframework.messaging.rsocket;

import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.PooledByteBufAllocator;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Access to strategies for use by RSocket requester and responder components.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface RSocketStrategies {

	/**
	 * Return the configured {@link Builder#encoder(Encoder[]) encoders}.
	 * @see #encoder(ResolvableType, MimeType)
	 */
	List<Encoder<?>> encoders();

	/**
	 * Find a compatible Encoder for the given element type.
	 * @param elementType the element type to match
	 * @param mimeType the MimeType to match
	 * @param <T> for casting the Encoder to the expected element type
	 * @return the matching Encoder
	 * @throws IllegalArgumentException if no matching Encoder is found
	 */
	@SuppressWarnings("unchecked")
	default <T> Encoder<T> encoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Encoder<?> encoder : encoders()) {
			if (encoder.canEncode(elementType, mimeType)) {
				return (Encoder<T>) encoder;
			}
		}
		throw new IllegalArgumentException("No encoder for " + elementType);
	}

	/**
	 * Return the configured {@link Builder#decoder(Decoder[]) decoders}.
	 * @see #decoder(ResolvableType, MimeType)
	 */
	List<Decoder<?>> decoders();

	/**
	 * Find a compatible Decoder for the given element type.
	 * @param elementType the element type to match
	 * @param mimeType the MimeType to match
	 * @param <T> for casting the Decoder to the expected element type
	 * @return the matching Decoder
	 * @throws IllegalArgumentException if no matching Decoder is found
	 */
	@SuppressWarnings("unchecked")
	default <T> Decoder<T> decoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Decoder<?> decoder : decoders()) {
			if (decoder.canDecode(elementType, mimeType)) {
				return (Decoder<T>) decoder;
			}
		}
		throw new IllegalArgumentException("No decoder for " + elementType);
	}

	/**
	 * Return the configured
	 * {@link Builder#reactiveAdapterStrategy(ReactiveAdapterRegistry) reactiveAdapterRegistry}.
	 */
	ReactiveAdapterRegistry reactiveAdapterRegistry();

	/**
	 * Return the configured
	 * {@link Builder#dataBufferFactory(DataBufferFactory) dataBufferFactory}.
	 */
	DataBufferFactory dataBufferFactory();


	/**
	 * Return a builder to build a new {@code RSocketStrategies} instance.
	 */
	static Builder builder() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder();
	}


	/**
	 * The builder options for creating {@code RSocketStrategies}.
	 */
	interface Builder {

		/**
		 * Add encoders to use for serializing Objects.
		 * <p>By default this is empty.
		 */
		Builder encoder(Encoder<?>... encoder);

		/**
		 * Add decoders for de-serializing Objects.
		 * <p>By default this is empty.
		 */
		Builder decoder(Decoder<?>... decoder);

		/**
		 * Access and manipulate the list of configured {@link #encoder encoders}.
		 */
		Builder encoders(Consumer<List<Encoder<?>>> consumer);

		/**
		 * Access and manipulate the list of configured {@link #encoder decoders}.
		 */
		Builder decoders(Consumer<List<Decoder<?>>> consumer);

		/**
		 * Configure the registry for reactive type support. This can be used to
		 * to adapt to, and/or determine the semantics of a given
		 * {@link org.reactivestreams.Publisher Publisher}.
		 * <p>By default this {@link ReactiveAdapterRegistry#sharedInstance}.
		 * @param registry the registry to use
		 */
		Builder reactiveAdapterStrategy(ReactiveAdapterRegistry registry);

		/**
		 * Configure the DataBufferFactory to use for allocating buffers, for
		 * example when preparing requests or when responding. The choice here
		 * must be aligned with the frame decoder configured in
		 * {@link io.rsocket.RSocketFactory}.
		 * <p>By default this property is an instance of
		 * {@link org.springframework.core.io.buffer.DefaultDataBufferFactory
		 * DefaultDataBufferFactory} matching to the default frame decoder in
		 * {@link io.rsocket.RSocketFactory} which copies the payload. This
		 * comes at cost to performance but does not require reference counting
		 * and eliminates possibility for memory leaks.
		 * <p>To switch to a zero-copy strategy,
		 * <a href="https://github.com/rsocket/rsocket-java#zero-copy">configure RSocket</a>
		 * accordingly, and then configure this property with an instance of
		 * {@link org.springframework.core.io.buffer.NettyDataBufferFactory
		 * NettyDataBufferFactory} with a pooled allocator such as
		 * {@link PooledByteBufAllocator#DEFAULT}.
		 * @param bufferFactory the DataBufferFactory to use
		 */
		Builder dataBufferFactory(DataBufferFactory bufferFactory);

		/**
		 * Builder the {@code RSocketStrategies} instance.
		 */
		RSocketStrategies build();
	}

}
