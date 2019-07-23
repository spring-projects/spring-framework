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

package org.springframework.messaging.rsocket;

import java.util.List;
import java.util.function.Consumer;

import io.rsocket.Payload;
import io.rsocket.RSocketFactory.ClientRSocketFactory;
import io.rsocket.RSocketFactory.ServerRSocketFactory;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
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
	 * Return a builder to create a new {@link RSocketStrategies} instance
	 * replicated from the current instance.
	 */
	default Builder mutate() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder(this);
	}


	/**
	 * The builder options for creating {@code RSocketStrategies}.
	 */
	interface Builder {

		/**
		 * Append to the list of encoders to use for serializing Objects to the
		 * data or metadata of a {@link Payload}.
		 * <p>By default this is initialized with encoders for {@code String},
		 * {@code byte[]}, {@code ByteBuffer}, and {@code DataBuffer}.
		 */
		Builder encoder(Encoder<?>... encoder);

		/**
		 * Apply the consumer to the list of configured encoders, immediately.
		 */
		Builder encoders(Consumer<List<Encoder<?>>> consumer);

		/**
		 * Append to the list of decoders to use for de-serializing Objects from
		 * the data or metadata of a {@link Payload}.
		 * <p>By default this is initialized with decoders for {@code String},
		 * {@code byte[]}, {@code ByteBuffer}, and {@code DataBuffer}.
		 */
		Builder decoder(Decoder<?>... decoder);

		/**
		 * Apply the consumer to the list of configured decoders, immediately.
		 */
		Builder decoders(Consumer<List<Decoder<?>>> consumer);

		/**
		 * Configure the registry for reactive type support. This can be used to
		 * to adapt to, and/or determine the semantics of a given
		 * {@link org.reactivestreams.Publisher Publisher}.
		 * <p>By default this {@link ReactiveAdapterRegistry#getSharedInstance()}.
		 */
		Builder reactiveAdapterStrategy(ReactiveAdapterRegistry registry);

		/**
		 * Configure the DataBufferFactory to use for allocating buffers when
		 * preparing requests or creating responses.
		 * <p>By default this is set to {@link NettyDataBufferFactory} with
		 * pooled, allocated buffers for zero copy. RSocket must also be
		 * <a href="https://github.com/rsocket/rsocket-java#zero-copy">configured</a>
		 * for zero copy. For client setup, {@link RSocketRequester.Builder}
		 * adapts automatically to the {@code DataBufferFactory} configured
		 * here, and sets the frame decoder in {@link ClientRSocketFactory
		 * ClientRSocketFactory} accordingly. For server setup, the
		 * {@link ServerRSocketFactory ServerRSocketFactory} must be configured
		 * accordingly too for zero copy.
		 * <p>If using {@link DefaultDataBufferFactory} instead, there is no
		 * need for related config changes in RSocket.
		 */
		Builder dataBufferFactory(DataBufferFactory bufferFactory);

		/**
		 * Build the {@code RSocketStrategies} instance.
		 */
		RSocketStrategies build();
	}

}
