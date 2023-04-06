/*
 * Copyright 2002-2023 the original author or authors.
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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;

/**
 * Default implementation of {@link RSocketStrategies}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.2
 */
final class DefaultRSocketStrategies implements RSocketStrategies {

	private final List<Encoder<?>> encoders;

	private final List<Decoder<?>> decoders;

	private final RouteMatcher routeMatcher;

	private final ReactiveAdapterRegistry adapterRegistry;

	private final DataBufferFactory bufferFactory;

	private final MetadataExtractor metadataExtractor;


	private DefaultRSocketStrategies(List<Encoder<?>> encoders, List<Decoder<?>> decoders,
			RouteMatcher routeMatcher, ReactiveAdapterRegistry adapterRegistry,
			DataBufferFactory bufferFactory, MetadataExtractor metadataExtractor) {

		this.encoders = Collections.unmodifiableList(encoders);
		this.decoders = Collections.unmodifiableList(decoders);
		this.routeMatcher = routeMatcher;
		this.adapterRegistry = adapterRegistry;
		this.bufferFactory = bufferFactory;
		this.metadataExtractor = metadataExtractor;
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
	public RouteMatcher routeMatcher() {
		return this.routeMatcher;
	}

	@Override
	public ReactiveAdapterRegistry reactiveAdapterRegistry() {
		return this.adapterRegistry;
	}

	@Override
	public DataBufferFactory dataBufferFactory() {
		return this.bufferFactory;
	}

	@Override
	public MetadataExtractor metadataExtractor() {
		return this.metadataExtractor;
	}


	/**
	 * Default implementation of {@link RSocketStrategies.Builder}.
	 */
	static class DefaultRSocketStrategiesBuilder implements RSocketStrategies.Builder {

		private final List<Encoder<?>> encoders = new ArrayList<>();

		private final List<Decoder<?>> decoders = new ArrayList<>();

		@Nullable
		private RouteMatcher routeMatcher;

		@Nullable
		private ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		@Nullable
		private DataBufferFactory bufferFactory;

		@Nullable
		private MetadataExtractor metadataExtractor;

		private final List<Consumer<MetadataExtractorRegistry>> metadataExtractors = new ArrayList<>();

		DefaultRSocketStrategiesBuilder() {
			this.encoders.add(CharSequenceEncoder.allMimeTypes());
			this.encoders.add(new ByteBufferEncoder());
			this.encoders.add(new ByteArrayEncoder());
			this.encoders.add(new DataBufferEncoder());

			// Order of decoders may be significant for default data MimeType
			// selection in RSocketRequester.Builder
			this.decoders.add(StringDecoder.allMimeTypes());
			this.decoders.add(new ByteBufferDecoder());
			this.decoders.add(new ByteArrayDecoder());
			this.decoders.add(new DataBufferDecoder());
		}

		DefaultRSocketStrategiesBuilder(RSocketStrategies other) {
			this.encoders.addAll(other.encoders());
			this.decoders.addAll(other.decoders());
			this.routeMatcher = other.routeMatcher();
			this.adapterRegistry = other.reactiveAdapterRegistry();
			this.bufferFactory = other.dataBufferFactory();
			this.metadataExtractor = other.metadataExtractor();
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
		public Builder routeMatcher(@Nullable RouteMatcher routeMatcher) {
			this.routeMatcher = routeMatcher;
			return this;
		}

		@Override
		public Builder reactiveAdapterStrategy(@Nullable ReactiveAdapterRegistry registry) {
			this.adapterRegistry = registry;
			return this;
		}

		@Override
		public Builder dataBufferFactory(@Nullable DataBufferFactory bufferFactory) {
			this.bufferFactory = bufferFactory;
			return this;
		}

		@Override
		public Builder metadataExtractor(@Nullable MetadataExtractor metadataExtractor) {
			this.metadataExtractor = metadataExtractor;
			return this;
		}

		@Override
		public Builder metadataExtractorRegistry(Consumer<MetadataExtractorRegistry> consumer) {
			this.metadataExtractors.add(consumer);
			return this;
		}

		@Override
		public RSocketStrategies build() {
			RouteMatcher matcher = (this.routeMatcher != null ? this.routeMatcher : initRouteMatcher());

			ReactiveAdapterRegistry registry = (this.adapterRegistry != null ?
					this.adapterRegistry : ReactiveAdapterRegistry.getSharedInstance());

			DataBufferFactory factory = (this.bufferFactory != null ?
					this.bufferFactory : new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT));

			MetadataExtractor extractor = (this.metadataExtractor != null ?
					this.metadataExtractor : new DefaultMetadataExtractor(this.decoders));

			if (extractor instanceof MetadataExtractorRegistry metadataExtractorRegistry) {
				this.metadataExtractors.forEach(consumer -> consumer.accept(metadataExtractorRegistry));
			}

			return new DefaultRSocketStrategies(
					this.encoders, this.decoders, matcher, registry, factory, extractor);
		}

		private RouteMatcher initRouteMatcher() {
			AntPathMatcher pathMatcher = new AntPathMatcher();
			pathMatcher.setPathSeparator(".");
			return new SimpleRouteMatcher(pathMatcher);
		}
	}

}
