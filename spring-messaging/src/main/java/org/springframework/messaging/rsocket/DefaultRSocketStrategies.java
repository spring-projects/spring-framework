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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.PooledByteBufAllocator;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default, package-private {@link RSocketStrategies} implementation.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
final class DefaultRSocketStrategies implements RSocketStrategies {

	private final List<Encoder<?>> encoders;

	private final List<Decoder<?>> decoders;

	private final ReactiveAdapterRegistry adapterRegistry;

	private final DataBufferFactory bufferFactory;


	private DefaultRSocketStrategies(List<Encoder<?>> encoders, List<Decoder<?>> decoders,
			ReactiveAdapterRegistry adapterRegistry, DataBufferFactory bufferFactory) {

		this.encoders = Collections.unmodifiableList(encoders);
		this.decoders = Collections.unmodifiableList(decoders);
		this.adapterRegistry = adapterRegistry;
		this.bufferFactory = bufferFactory;
	}


	@Override
	public List<Encoder<?>> encoders() {
		return this.encoders;
	}

	@Override
	public List<Decoder<?>> decoders() {
		return this.decoders;
	}

	@Override
	public ReactiveAdapterRegistry reactiveAdapterRegistry() {
		return this.adapterRegistry;
	}

	@Override
	public DataBufferFactory dataBufferFactory() {
		return this.bufferFactory;
	}


	/**
	 * Default RSocketStrategies.Builder implementation.
	 */
	static class DefaultRSocketStrategiesBuilder implements RSocketStrategies.Builder {

		private final List<Encoder<?>> encoders = new ArrayList<>();

		private final List<Decoder<?>> decoders = new ArrayList<>();

		private ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		@Nullable
		private DataBufferFactory bufferFactory;


		DefaultRSocketStrategiesBuilder() {

			// Order of decoders may be significant for default data MimeType
			// selection in RSocketRequester.Builder

			this.decoders.add(StringDecoder.allMimeTypes());
			this.decoders.add(new ByteBufferDecoder());
			this.decoders.add(new ByteArrayDecoder());
			this.decoders.add(new DataBufferDecoder());

			this.encoders.add(CharSequenceEncoder.allMimeTypes());
			this.encoders.add(new ByteBufferEncoder());
			this.encoders.add(new ByteArrayEncoder());
			this.encoders.add(new DataBufferEncoder());
		}

		DefaultRSocketStrategiesBuilder(RSocketStrategies other) {
			this.encoders.addAll(other.encoders());
			this.decoders.addAll(other.decoders());
			this.adapterRegistry = other.reactiveAdapterRegistry();
			this.bufferFactory = other.dataBufferFactory();
		}


		@Override
		public Builder encoder(Encoder<?>... encoders) {
			this.encoders.addAll(Arrays.asList(encoders));
			return this;
		}

		@Override
		public Builder decoder(Decoder<?>... decoder) {
			this.decoders.addAll(Arrays.asList(decoder));
			return this;
		}

		@Override
		public Builder encoders(Consumer<List<Encoder<?>>> consumer) {
			consumer.accept(this.encoders);
			return this;
		}

		@Override
		public Builder decoders(Consumer<List<Decoder<?>>> consumer) {
			consumer.accept(this.decoders);
			return this;
		}

		@Override
		public Builder reactiveAdapterStrategy(ReactiveAdapterRegistry registry) {
			Assert.notNull(registry, "ReactiveAdapterRegistry is required");
			this.adapterRegistry = registry;
			return this;
		}

		@Override
		public Builder dataBufferFactory(DataBufferFactory bufferFactory) {
			this.bufferFactory = bufferFactory;
			return this;
		}

		@Override
		public RSocketStrategies build() {
			return new DefaultRSocketStrategies(
					this.encoders, this.decoders, this.adapterRegistry, initBufferFactory());
		}

		private DataBufferFactory initBufferFactory() {
			return this.bufferFactory != null ? this.bufferFactory :
					new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);
		}
	}

}
