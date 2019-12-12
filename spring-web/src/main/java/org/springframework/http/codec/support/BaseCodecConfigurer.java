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

package org.springframework.http.codec.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link CodecConfigurer} that serves as a base for
 * client and server specific variants.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
abstract class BaseCodecConfigurer implements CodecConfigurer {

	protected final BaseDefaultCodecs defaultCodecs;

	protected final DefaultCustomCodecs customCodecs;


	/**
	 * Constructor with the base {@link BaseDefaultCodecs} to use, which can be
	 * a client or server specific variant.
	 */
	BaseCodecConfigurer(BaseDefaultCodecs defaultCodecs) {
		Assert.notNull(defaultCodecs, "'defaultCodecs' is required");
		this.defaultCodecs = defaultCodecs;
		this.customCodecs = new DefaultCustomCodecs();
	}

	/**
	 * Create a deep copy of the given {@link BaseCodecConfigurer}.
	 * @since 5.1.12
	 */
	protected BaseCodecConfigurer(BaseCodecConfigurer other) {
		this.defaultCodecs = other.cloneDefaultCodecs();
		this.customCodecs = new DefaultCustomCodecs(other.customCodecs);
	}

	/**
	 * Sub-classes should override this to create  deep copy of
	 * {@link BaseDefaultCodecs} which can can be client or server specific.
	 * @since 5.1.12
	 */
	protected abstract BaseDefaultCodecs cloneDefaultCodecs();


	@Override
	public DefaultCodecs defaultCodecs() {
		return this.defaultCodecs;
	}

	@Override
	public void registerDefaults(boolean shouldRegister) {
		this.defaultCodecs.registerDefaults(shouldRegister);
	}

	@Override
	public CustomCodecs customCodecs() {
		return this.customCodecs;
	}

	@Override
	public List<HttpMessageReader<?>> getReaders() {
		this.defaultCodecs.applyDefaultConfig(this.customCodecs);

		List<HttpMessageReader<?>> result = new ArrayList<>();
		result.addAll(this.customCodecs.getTypedReaders().keySet());
		result.addAll(this.defaultCodecs.getTypedReaders());
		result.addAll(this.customCodecs.getObjectReaders().keySet());
		result.addAll(this.defaultCodecs.getObjectReaders());
		result.addAll(this.defaultCodecs.getCatchAllReaders());
		return result;
	}

	@Override
	public List<HttpMessageWriter<?>> getWriters() {
		this.defaultCodecs.applyDefaultConfig(this.customCodecs);

		List<HttpMessageWriter<?>> result = new ArrayList<>();
		result.addAll(this.customCodecs.getTypedWriters().keySet());
		result.addAll(this.defaultCodecs.getTypedWriters());
		result.addAll(this.customCodecs.getObjectWriters().keySet());
		result.addAll(this.defaultCodecs.getObjectWriters());
		result.addAll(this.defaultCodecs.getCatchAllWriters());
		return result;
	}

	@Override
	public abstract CodecConfigurer clone();


	/**
	 * Default implementation of {@code CustomCodecs}.
	 */
	protected static final class DefaultCustomCodecs implements CustomCodecs {

		private final Map<HttpMessageReader<?>, Boolean> typedReaders = new LinkedHashMap<>(4);

		private final Map<HttpMessageWriter<?>, Boolean> typedWriters = new LinkedHashMap<>(4);

		private final Map<HttpMessageReader<?>, Boolean> objectReaders = new LinkedHashMap<>(4);

		private final Map<HttpMessageWriter<?>, Boolean> objectWriters = new LinkedHashMap<>(4);

		private final List<Consumer<DefaultCodecConfig>> defaultConfigConsumers = new ArrayList<>(4);

		DefaultCustomCodecs() {
		}

		/**
		 * Create a deep copy of the given {@link DefaultCustomCodecs}.
		 * @since 5.1.12
		 */
		DefaultCustomCodecs(DefaultCustomCodecs other) {
			other.typedReaders.putAll(this.typedReaders);
			other.typedWriters.putAll(this.typedWriters);
			other.objectReaders.putAll(this.objectReaders);
			other.objectWriters.putAll(this.objectWriters);
		}

		@Override
		public void register(Object codec) {
			addCodec(codec, false);
		}

		@Override
		public void registerWithDefaultConfig(Object codec) {
			addCodec(codec, true);
		}

		@Override
		public void registerWithDefaultConfig(Object codec, Consumer<DefaultCodecConfig> configConsumer) {
			addCodec(codec, false);
			this.defaultConfigConsumers.add(configConsumer);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void decoder(Decoder<?> decoder) {
			addCodec(decoder, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void encoder(Encoder<?> encoder) {
			addCodec(encoder, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void reader(HttpMessageReader<?> reader) {
			addCodec(reader, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void writer(HttpMessageWriter<?> writer) {
			addCodec(writer, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void withDefaultCodecConfig(Consumer<DefaultCodecConfig> codecsConfigConsumer) {
			this.defaultConfigConsumers.add(codecsConfigConsumer);
		}

		private void addCodec(Object codec, boolean applyDefaultConfig) {

			if (codec instanceof Decoder) {
				codec = new DecoderHttpMessageReader<>((Decoder<?>) codec);
			}
			else if (codec instanceof Encoder) {
				codec = new EncoderHttpMessageWriter<>((Encoder<?>) codec);
			}

			if (codec instanceof HttpMessageReader) {
				HttpMessageReader<?> reader = (HttpMessageReader<?>) codec;
				boolean canReadToObject = reader.canRead(ResolvableType.forClass(Object.class), null);
				(canReadToObject ? this.objectReaders : this.typedReaders).put(reader, applyDefaultConfig);
			}
			else if (codec instanceof HttpMessageWriter) {
				HttpMessageWriter<?> writer = (HttpMessageWriter<?>) codec;
				boolean canWriteObject = writer.canWrite(ResolvableType.forClass(Object.class), null);
				(canWriteObject ? this.objectWriters : this.typedWriters).put(writer, applyDefaultConfig);
			}
			else {
				throw new IllegalArgumentException("Unexpected codec type: " + codec.getClass().getName());
			}
		}

		// Package private accessors...

		Map<HttpMessageReader<?>, Boolean> getTypedReaders() {
			return this.typedReaders;
		}

		Map<HttpMessageWriter<?>, Boolean> getTypedWriters() {
			return this.typedWriters;
		}

		Map<HttpMessageReader<?>, Boolean> getObjectReaders() {
			return this.objectReaders;
		}

		Map<HttpMessageWriter<?>, Boolean> getObjectWriters() {
			return this.objectWriters;
		}

		List<Consumer<DefaultCodecConfig>> getDefaultConfigConsumers() {
			return this.defaultConfigConsumers;
		}
	}

}
