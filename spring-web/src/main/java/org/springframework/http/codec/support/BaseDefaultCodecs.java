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
import java.util.Collections;
import java.util.List;

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
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link CodecConfigurer.DefaultCodecs} that serves
 * as a base for client and server specific variants.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class BaseDefaultCodecs implements CodecConfigurer.DefaultCodecs {

	static final boolean jackson2Present;

	private static final boolean jackson2SmilePresent;

	private static final boolean jaxb2Present;

	private static final boolean protobufPresent;

	static {
		ClassLoader classLoader = BaseCodecConfigurer.class.getClassLoader();
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		protobufPresent = ClassUtils.isPresent("com.google.protobuf.Message", classLoader);
	}


	@Nullable
	private Decoder<?> jackson2JsonDecoder;

	@Nullable
	private Encoder<?> jackson2JsonEncoder;

	@Nullable
	private Decoder<?> protobufDecoder;

	@Nullable
	private Encoder<?> protobufEncoder;

	@Nullable
	private Decoder<?> jaxb2Decoder;

	@Nullable
	private Encoder<?> jaxb2Encoder;

	private boolean enableLoggingRequestDetails = false;

	private boolean registerDefaults = true;


	@Override
	public void jackson2JsonDecoder(Decoder<?> decoder) {
		this.jackson2JsonDecoder = decoder;
	}

	@Override
	public void jackson2JsonEncoder(Encoder<?> encoder) {
		this.jackson2JsonEncoder = encoder;
	}

	@Override
	public void protobufDecoder(Decoder<?> decoder) {
		this.protobufDecoder = decoder;
	}

	@Override
	public void protobufEncoder(Encoder<?> encoder) {
		this.protobufEncoder = encoder;
	}

	@Override
	public void jaxb2Decoder(Decoder<?> decoder) {
		this.jaxb2Decoder = decoder;
	}

	@Override
	public void jaxb2Encoder(Encoder<?> encoder) {
		this.jaxb2Encoder = encoder;
	}

	@Override
	public void enableLoggingRequestDetails(boolean enable) {
		this.enableLoggingRequestDetails = enable;
	}

	protected boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

	/**
	 * Delegate method used from {@link BaseCodecConfigurer#registerDefaults}.
	 */
	void registerDefaults(boolean registerDefaults) {
		this.registerDefaults = registerDefaults;
	}


	/**
	 * Return readers that support specific types.
	 */
	final List<HttpMessageReader<?>> getTypedReaders() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		readers.add(new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		readers.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		readers.add(new DecoderHttpMessageReader<>(new DataBufferDecoder()));
		readers.add(new ResourceHttpMessageReader());
		readers.add(new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
		if (protobufPresent) {
			Decoder<?> decoder = this.protobufDecoder != null ? this.protobufDecoder : new ProtobufDecoder();
			readers.add(new DecoderHttpMessageReader<>(decoder));
		}

		FormHttpMessageReader formReader = new FormHttpMessageReader();
		formReader.setEnableLoggingRequestDetails(this.enableLoggingRequestDetails);
		readers.add(formReader);

		extendTypedReaders(readers);

		return readers;
	}

	/**
	 * Hook for client or server specific typed readers.
	 */
	protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) {
	}

	/**
	 * Return Object readers (JSON, XML, SSE).
	 */
	final List<HttpMessageReader<?>> getObjectReaders() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		if (jackson2Present) {
			readers.add(new DecoderHttpMessageReader<>(getJackson2JsonDecoder()));
		}
		if (jackson2SmilePresent) {
			readers.add(new DecoderHttpMessageReader<>(new Jackson2SmileDecoder()));
		}
		if (jaxb2Present) {
			Decoder<?> decoder = this.jaxb2Decoder != null ? this.jaxb2Decoder : new Jaxb2XmlDecoder();
			readers.add(new DecoderHttpMessageReader<>(decoder));
		}
		extendObjectReaders(readers);
		return readers;
	}

	/**
	 * Hook for client or server specific Object readers.
	 */
	protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {
	}

	/**
	 * Return readers that need to be at the end, after all others.
	 */
	final List<HttpMessageReader<?>> getCatchAllReaders() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageReader<?>> result = new ArrayList<>();
		result.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		return result;
	}

	/**
	 * Return writers that support specific types.
	 * @param forMultipart whether to returns writers for general use ("false"),
	 * or for multipart requests only ("true"). Generally the two sets are the
	 * same except for the multipart writer itself.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	final List<HttpMessageWriter<?>> getTypedWriters(boolean forMultipart) {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		writers.add(new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
		writers.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		writers.add(new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
		writers.add(new ResourceHttpMessageWriter());
		writers.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		// No client or server specific multipart writers currently..
		if (!forMultipart) {
			extendTypedWriters(writers);
		}
		if (protobufPresent) {
			Encoder<?> encoder = this.protobufEncoder != null ? this.protobufEncoder : new ProtobufEncoder();
			writers.add(new ProtobufHttpMessageWriter((Encoder) encoder));
		}
		return writers;
	}

	/**
	 * Hook for client or server specific typed writers.
	 */
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
	}

	/**
	 * Return Object writers (JSON, XML, SSE).
	 * @param forMultipart whether to returns writers for general use ("false"),
	 * or for multipart requests only ("true"). Generally the two sets are the
	 * same except for the multipart writer itself.
	 */
	final List<HttpMessageWriter<?>> getObjectWriters(boolean forMultipart) {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		if (jackson2Present) {
			writers.add(new EncoderHttpMessageWriter<>(getJackson2JsonEncoder()));
		}
		if (jackson2SmilePresent) {
			writers.add(new EncoderHttpMessageWriter<>(new Jackson2SmileEncoder()));
		}
		if (jaxb2Present) {
			Encoder<?> encoder = this.jaxb2Encoder != null ? this.jaxb2Encoder : new Jaxb2XmlEncoder();
			writers.add(new EncoderHttpMessageWriter<>(encoder));
		}
		// No client or server specific multipart writers currently..
		if (!forMultipart) {
			extendObjectWriters(writers);
		}
		return writers;
	}

	/**
	 * Hook for client or server specific Object writers.
	 */
	protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
	}

	/**
	 * Return writers that need to be at the end, after all others.
	 */
	List<HttpMessageWriter<?>> getCatchAllWriters() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> result = new ArrayList<>();
		result.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		return result;
	}


	// Accessors for use in subclasses...

	protected Decoder<?> getJackson2JsonDecoder() {
		return (this.jackson2JsonDecoder != null ? this.jackson2JsonDecoder : new Jackson2JsonDecoder());
	}

	protected Encoder<?> getJackson2JsonEncoder() {
		return (this.jackson2JsonEncoder != null ? this.jackson2JsonEncoder : new Jackson2JsonEncoder());
	}

}
