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
import java.util.Collections;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link CodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
abstract class AbstractCodecConfigurer implements CodecConfigurer {

	static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					AbstractCodecConfigurer.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							AbstractCodecConfigurer.class.getClassLoader());

	private static final boolean jackson2SmilePresent =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory",
					AbstractCodecConfigurer.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", AbstractCodecConfigurer.class.getClassLoader());


	private final AbstractDefaultCodecs defaultCodecs;

	private final DefaultCustomCodecs customCodecs = new DefaultCustomCodecs();


	AbstractCodecConfigurer(AbstractDefaultCodecs defaultCodecs) {
		Assert.notNull(defaultCodecs, "'defaultCodecs' is required");
		this.defaultCodecs = defaultCodecs;
		this.defaultCodecs.setCustomCodecs(this.customCodecs);
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
		List<HttpMessageWriter<?>> result = new ArrayList<>();

		result.addAll(this.defaultCodecs.getTypedWriters());
		result.addAll(this.customCodecs.getTypedWriters());

		result.addAll(this.defaultCodecs.getObjectWriters());
		result.addAll(this.customCodecs.getObjectWriters());

		result.addAll(this.defaultCodecs.getCatchAllWriters());
		return result;
	}


	abstract static class AbstractDefaultCodecs implements DefaultCodecs {

		private boolean registerDefaults = true;

		@Nullable
		private Decoder<?> jackson2JsonDecoder;

		@Nullable
		private Encoder<?> jackson2JsonEncoder;

		@Nullable
		private DefaultCustomCodecs customCodecs;

		void registerDefaults(boolean registerDefaults) {
			this.registerDefaults = registerDefaults;
		}

		boolean shouldRegisterDefaults() {
			return this.registerDefaults;
		}

		/**
		 * Access to custom codecs for subclasses, e.g. for multipart writers.
		 */
		void setCustomCodecs(@Nullable DefaultCustomCodecs customCodecs) {
			this.customCodecs = customCodecs;
		}

		@Nullable
		DefaultCustomCodecs getCustomCodecs() {
			return this.customCodecs;
		}

		@Override
		public void jackson2JsonDecoder(Decoder<?> decoder) {
			this.jackson2JsonDecoder = decoder;
		}

		Decoder<?> getJackson2JsonDecoder() {
			return (this.jackson2JsonDecoder != null ? this.jackson2JsonDecoder : new Jackson2JsonDecoder());
		}

		@Override
		public void jackson2JsonEncoder(Encoder<?> encoder) {
			this.jackson2JsonEncoder = encoder;
		}

		Encoder<?> getJackson2JsonEncoder() {
			return (this.jackson2JsonEncoder != null ? this.jackson2JsonEncoder : new Jackson2JsonEncoder());
		}

		// Readers...

		List<HttpMessageReader<?>> getTypedReaders() {
			if (!this.registerDefaults) {
				return Collections.emptyList();
			}
			List<HttpMessageReader<?>> result = new ArrayList<>();
			result.add(new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
			result.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
			result.add(new DecoderHttpMessageReader<>(new DataBufferDecoder()));
			result.add(new DecoderHttpMessageReader<>(new ResourceDecoder()));
			result.add(new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
			return result;
		}

		List<HttpMessageReader<?>> getObjectReaders() {
			if (!this.registerDefaults) {
				return Collections.emptyList();
			}
			List<HttpMessageReader<?>> result = new ArrayList<>();
			if (jackson2Present) {
				result.add(new DecoderHttpMessageReader<>(getJackson2JsonDecoder()));
			}
			if (jackson2SmilePresent) {
				result.add(new DecoderHttpMessageReader<>(new Jackson2SmileDecoder()));
			}
			if (jaxb2Present) {
				result.add(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
			}
			return result;
		}

		List<HttpMessageReader<?>> getCatchAllReaders() {
			if (!this.registerDefaults) {
				return Collections.emptyList();
			}
			List<HttpMessageReader<?>> result = new ArrayList<>();
			result.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
			return result;
		}

		// Writers...

		List<HttpMessageWriter<?>> getTypedWriters() {
			if (!this.registerDefaults) {
				return Collections.emptyList();
			}
			List<HttpMessageWriter<?>> result = new ArrayList<>();
			result.add(new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
			result.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
			result.add(new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
			result.add(new ResourceHttpMessageWriter());
			result.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
			return result;
		}

		List<HttpMessageWriter<?>> getObjectWriters() {
			if (!this.registerDefaults) {
				return Collections.emptyList();
			}
			List<HttpMessageWriter<?>> result = new ArrayList<>();
			if (jackson2Present) {
				result.add(new EncoderHttpMessageWriter<>(getJackson2JsonEncoder()));
			}
			if (jackson2SmilePresent) {
				result.add(new EncoderHttpMessageWriter<>(new Jackson2SmileEncoder()));
			}
			if (jaxb2Present) {
				result.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
			}
			return result;
		}

		List<HttpMessageWriter<?>> getCatchAllWriters() {
			if (!this.registerDefaults) {
				return Collections.emptyList();
			}
			List<HttpMessageWriter<?>> result = new ArrayList<>();
			result.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
			return result;
		}
	}


	static class DefaultCustomCodecs implements CustomCodecs {

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
