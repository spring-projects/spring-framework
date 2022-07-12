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

package org.springframework.messaging.rsocket;

import java.util.List;
import java.util.function.Consumer;

import io.rsocket.Payload;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MimeType;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;

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
	 * Return the configured {@link Builder#routeMatcher(RouteMatcher)}.
	 */
	RouteMatcher routeMatcher();

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
	 * Return the configured {@link Builder#metadataExtractor(MetadataExtractor)}.
	 */
	MetadataExtractor metadataExtractor();

	/**
	 * Return a builder to create a new {@link RSocketStrategies} instance
	 * replicated from the current instance.
	 */
	default Builder mutate() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder(this);
	}


	/**
	 * Create an {@code RSocketStrategies} instance with default settings.
	 * Equivalent to {@code RSocketStrategies.builder().build()}. See individual
	 * builder methods for details on default settings.
	 */
	static RSocketStrategies create() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder().build();
	}

	/**
	 * Return a builder to prepare a new {@code RSocketStrategies} instance.
	 * The builder applies default settings, see individual builder methods for
	 * details.
	 */
	static Builder builder() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder();
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
		 * Configure a {@code RouteMatcher} for matching routes to message
		 * handlers based on route patterns. This option is applicable to
		 * client or server responders.
		 * <p>By default, {@link SimpleRouteMatcher} is used, backed by
		 * {@link AntPathMatcher} with "." as separator. For better
		 * efficiency consider switching to {@code PathPatternRouteMatcher} from
		 * {@code spring-web} instead.
		 */
		Builder routeMatcher(@Nullable RouteMatcher routeMatcher);

		/**
		 * Configure the registry for reactive type support. This can be used
		 * to adapt to, and/or determine the semantics of a given
		 * {@link org.reactivestreams.Publisher Publisher}.
		 * <p>By default this {@link ReactiveAdapterRegistry#getSharedInstance()}.
		 */
		Builder reactiveAdapterStrategy(@Nullable ReactiveAdapterRegistry registry);

		/**
		 * Configure the DataBufferFactory to use for allocating buffers when
		 * preparing requests or creating responses.
		 * <p>By default this is set to {@link NettyDataBufferFactory} with
		 * pooled, allocated buffers for zero copy. RSocket must also be
		 * <a href="https://github.com/rsocket/rsocket-java#zero-copy">configured</a>
		 * for zero copy. For client setup, {@link RSocketRequester.Builder}
		 * adapts automatically to the {@code DataBufferFactory} configured
		 * here, and sets the frame decoder in
		 * {@link io.rsocket.core.RSocketConnector RSocketConnector}
		 * accordingly. For server setup, the
		 * {@link io.rsocket.core.RSocketServer RSocketServer} must be configured
		 * accordingly for zero copy too.
		 * <p>If using {@link DefaultDataBufferFactory} instead, there is no
		 * need for related config changes in RSocket.
		 */
		Builder dataBufferFactory(@Nullable DataBufferFactory bufferFactory);

		/**
		 * Configure a {@link MetadataExtractor} to extract the route along with
		 * other metadata. This option is applicable to client or server
		 * responders.
		 * <p>By default this is {@link DefaultMetadataExtractor} created with
		 * the {@link #decoder(Decoder[]) configured} decoders and extracting a
		 * route from {@code "message/x.rsocket.routing.v0"} metadata.
		 */
		Builder metadataExtractor(@Nullable MetadataExtractor metadataExtractor);

		/**
		 * Apply the consumer to the {@link MetadataExtractorRegistry} in order
		 * to register extra metadata entry extractors.
		 */
		Builder metadataExtractorRegistry(Consumer<MetadataExtractorRegistry> consumer);

		/**
		 * Build the {@code RSocketStrategies} instance.
		 */
		RSocketStrategies build();
	}

}
