/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec.support;

import java.util.ArrayList;
import java.util.List;

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
 * @since 5.0
 */
class BaseCodecConfigurer implements CodecConfigurer {

	private final BaseDefaultCodecs defaultCodecs;

	private final DefaultCustomCodecs customCodecs = new DefaultCustomCodecs();


	/**
	 * Constructor with the base {@link BaseDefaultCodecs} to use, which can be
	 * a client or server specific variant.
	 */
	BaseCodecConfigurer(BaseDefaultCodecs defaultCodecs) {
		Assert.notNull(defaultCodecs, "'defaultCodecs' is required");
		this.defaultCodecs = defaultCodecs;
	}


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
		List<HttpMessageReader<?>> result = new ArrayList<>();

		result.addAll(this.defaultCodecs.getTypedReaders());
		result.addAll(this.customCodecs.getTypedReaders());

		result.addAll(this.defaultCodecs.getObjectReaders());
		result.addAll(this.customCodecs.getObjectReaders());

		result.addAll(this.defaultCodecs.getCatchAllReaders());
		return result;
	}

	@Override
	public List<HttpMessageWriter<?>> getWriters() {
		return getWritersInternal(false);
	}

	/**
	 * Internal method that returns the configured writers.
	 * @param forMultipart whether to returns writers for general use ("false"),
	 * or for multipart requests only ("true"). Generally the two sets are the
	 * same except for the multipart writer itself.
	 */
	protected List<HttpMessageWriter<?>> getWritersInternal(boolean forMultipart) {
		List<HttpMessageWriter<?>> result = new ArrayList<>();

		result.addAll(this.defaultCodecs.getTypedWriters(forMultipart));
		result.addAll(this.customCodecs.getTypedWriters());

		result.addAll(this.defaultCodecs.getObjectWriters(forMultipart));
		result.addAll(this.customCodecs.getObjectWriters());

		result.addAll(this.defaultCodecs.getCatchAllWriters());
		return result;
	}


	/**
	 * Default implementation of {@code CustomCodecs}.
	 */
	private static final class DefaultCustomCodecs implements CustomCodecs {

		private final List<HttpMessageReader<?>> typedReaders = new ArrayList<>();

		private final List<HttpMessageWriter<?>> typedWriters = new ArrayList<>();

		private final List<HttpMessageReader<?>> objectReaders = new ArrayList<>();

		private final List<HttpMessageWriter<?>> objectWriters = new ArrayList<>();


		@Override
		public void decoder(Decoder<?> decoder) {
			reader(new DecoderHttpMessageReader<>(decoder));
		}

		@Override
		public void encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
		}

		@Override
		public void reader(HttpMessageReader<?> reader) {
			boolean canReadToObject = reader.canRead(ResolvableType.forClass(Object.class), null);
			(canReadToObject ? this.objectReaders : this.typedReaders).add(reader);
		}

		@Override
		public void writer(HttpMessageWriter<?> writer) {
			boolean canWriteObject = writer.canWrite(ResolvableType.forClass(Object.class), null);
			(canWriteObject ? this.objectWriters : this.typedWriters).add(writer);
		}


		// Package private accessors...

		List<HttpMessageReader<?>> getTypedReaders() {
			return this.typedReaders;
		}

		List<HttpMessageWriter<?>> getTypedWriters() {
			return this.typedWriters;
		}

		List<HttpMessageReader<?>> getObjectReaders() {
			return this.objectReaders;
		}

		List<HttpMessageWriter<?>> getObjectWriters() {
			return this.objectWriters;
		}
	}

}
