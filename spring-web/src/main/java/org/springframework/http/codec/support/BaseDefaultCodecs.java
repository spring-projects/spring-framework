/*
 * Copyright 2002-present the original author or authors.
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.NettyByteBufDecoder;
import org.springframework.core.codec.NettyByteBufEncoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.AbstractJacksonDecoder;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.codec.cbor.JacksonCborDecoder;
import org.springframework.http.codec.cbor.JacksonCborEncoder;
import org.springframework.http.codec.cbor.KotlinSerializationCborDecoder;
import org.springframework.http.codec.cbor.KotlinSerializationCborEncoder;
import org.springframework.http.codec.json.AbstractJackson2Decoder;
import org.springframework.http.codec.json.GsonDecoder;
import org.springframework.http.codec.json.GsonEncoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder;
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.multipart.PartEventHttpMessageReader;
import org.springframework.http.codec.multipart.PartEventHttpMessageWriter;
import org.springframework.http.codec.multipart.PartHttpMessageWriter;
import org.springframework.http.codec.protobuf.KotlinSerializationProtobufDecoder;
import org.springframework.http.codec.protobuf.KotlinSerializationProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.smile.JacksonSmileDecoder;
import org.springframework.http.codec.smile.JacksonSmileEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of {@link CodecConfigurer.DefaultCodecs} that serves
 * as a base for client and server specific variants.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class BaseDefaultCodecs implements CodecConfigurer.DefaultCodecs, CodecConfigurer.DefaultCodecConfig {

	static final boolean JACKSON_PRESENT;

	static final boolean JACKSON_2_PRESENT;

	static final boolean GSON_PRESENT;

	private static final boolean JACKSON_SMILE_PRESENT;

	private static final boolean JACKSON_2_SMILE_PRESENT;

	private static final boolean JACKSON_CBOR_PRESENT;

	private static final boolean JACKSON_2_CBOR_PRESENT;

	private static final boolean JAXB_2_PRESENT;

	private static final boolean PROTOBUF_PRESENT;

	static final boolean NETTY_BYTE_BUF_PRESENT;

	static final boolean KOTLIN_SERIALIZATION_CBOR_PRESENT;

	static final boolean KOTLIN_SERIALIZATION_JSON_PRESENT;

	static final boolean KOTLIN_SERIALIZATION_PROTOBUF_PRESENT;

	static {
		ClassLoader classLoader = BaseCodecConfigurer.class.getClassLoader();
		JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
		JACKSON_2_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		GSON_PRESENT = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		JACKSON_SMILE_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
		JACKSON_2_SMILE_PRESENT = JACKSON_2_PRESENT && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		JACKSON_CBOR_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
		JACKSON_2_CBOR_PRESENT = JACKSON_2_PRESENT && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper", classLoader);
		JAXB_2_PRESENT = ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
		PROTOBUF_PRESENT = ClassUtils.isPresent("com.google.protobuf.Message", classLoader);
		NETTY_BYTE_BUF_PRESENT = ClassUtils.isPresent("io.netty.buffer.ByteBuf", classLoader);
		KOTLIN_SERIALIZATION_CBOR_PRESENT = ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", classLoader);
		KOTLIN_SERIALIZATION_JSON_PRESENT = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
		KOTLIN_SERIALIZATION_PROTOBUF_PRESENT = ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", classLoader);
	}


	private @Nullable Decoder<?> jacksonJsonDecoder;

	private @Nullable Encoder<?> jacksonJsonEncoder;

	private @Nullable Decoder<?> gsonDecoder;

	private @Nullable Encoder<?> gsonEncoder;

	private @Nullable Encoder<?> jacksonSmileEncoder;

	private @Nullable Decoder<?> jacksonSmileDecoder;

	private @Nullable Encoder<?> jacksonCborEncoder;

	private @Nullable Decoder<?> jacksonCborDecoder;

	private @Nullable Decoder<?> protobufDecoder;

	private @Nullable Encoder<?> protobufEncoder;

	private @Nullable Decoder<?> jaxb2Decoder;

	private @Nullable Encoder<?> jaxb2Encoder;

	private @Nullable Decoder<?> kotlinSerializationCborDecoder;

	private @Nullable Encoder<?> kotlinSerializationCborEncoder;

	private @Nullable Decoder<?> kotlinSerializationJsonDecoder;

	private @Nullable Encoder<?> kotlinSerializationJsonEncoder;

	private @Nullable Decoder<?> kotlinSerializationProtobufDecoder;

	private @Nullable Encoder<?> kotlinSerializationProtobufEncoder;

	private @Nullable DefaultMultipartCodecs multipartCodecs;

	private @Nullable Supplier<List<HttpMessageWriter<?>>> partWritersSupplier;

	private @Nullable HttpMessageReader<?> multipartReader;

	private @Nullable Consumer<Object> codecConsumer;

	private @Nullable Integer maxInMemorySize;

	private @Nullable Boolean enableLoggingRequestDetails;

	private boolean registerDefaults = true;


	// The default reader and writer instances to use

	private final List<HttpMessageReader<?>> typedReaders = new ArrayList<>();

	private final List<HttpMessageReader<?>> objectReaders = new ArrayList<>();

	private final List<HttpMessageWriter<?>> typedWriters = new ArrayList<>();

	private final List<HttpMessageWriter<?>> objectWriters = new ArrayList<>();


	BaseDefaultCodecs() {
		initReaders();
		initWriters();
	}

	/**
	 * Reset and initialize typed readers and object readers.
	 * @since 5.3.3
	 */
	protected void initReaders() {
		initTypedReaders();
		initObjectReaders();
	}

	/**
	 * Reset and initialize typed writers and object writers.
	 * @since 5.3.3
	 */
	protected void initWriters() {
		initTypedWriters();
		initObjectWriters();
	}

	/**
	 * Create a deep copy of the given {@link BaseDefaultCodecs}.
	 */
	protected BaseDefaultCodecs(BaseDefaultCodecs other) {
		this.jacksonJsonDecoder = other.jacksonJsonDecoder;
		this.jacksonJsonEncoder = other.jacksonJsonEncoder;
		this.gsonDecoder = other.gsonDecoder;
		this.gsonEncoder = other.gsonEncoder;
		this.jacksonSmileDecoder = other.jacksonSmileDecoder;
		this.jacksonSmileEncoder = other.jacksonSmileEncoder;
		this.jacksonCborDecoder = other.jacksonCborDecoder;
		this.jacksonCborEncoder = other.jacksonCborEncoder;
		this.protobufDecoder = other.protobufDecoder;
		this.protobufEncoder = other.protobufEncoder;
		this.jaxb2Decoder = other.jaxb2Decoder;
		this.jaxb2Encoder = other.jaxb2Encoder;
		this.kotlinSerializationCborDecoder = other.kotlinSerializationCborDecoder;
		this.kotlinSerializationCborEncoder = other.kotlinSerializationCborEncoder;
		this.kotlinSerializationJsonDecoder = other.kotlinSerializationJsonDecoder;
		this.kotlinSerializationJsonEncoder = other.kotlinSerializationJsonEncoder;
		this.kotlinSerializationProtobufDecoder = other.kotlinSerializationProtobufDecoder;
		this.kotlinSerializationProtobufEncoder = other.kotlinSerializationProtobufEncoder;
		this.multipartCodecs = other.multipartCodecs != null ?
				new DefaultMultipartCodecs(other.multipartCodecs) : null;
		this.multipartReader = other.multipartReader;
		this.codecConsumer = other.codecConsumer;
		this.maxInMemorySize = other.maxInMemorySize;
		this.enableLoggingRequestDetails = other.enableLoggingRequestDetails;
		this.registerDefaults = other.registerDefaults;
		this.typedReaders.addAll(other.typedReaders);
		this.objectReaders.addAll(other.objectReaders);
		this.typedWriters.addAll(other.typedWriters);
		this.objectWriters.addAll(other.objectWriters);
	}

	@Override
	public void jacksonJsonDecoder(Decoder<?> decoder) {
		this.jacksonJsonDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void jacksonJsonEncoder(Encoder<?> encoder) {
		this.jacksonJsonEncoder = encoder;
		initObjectWriters();
		initTypedWriters();
	}

	@Override
	public void gsonDecoder(Decoder<?> decoder) {
		this.gsonDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void gsonEncoder(Encoder<?> encoder) {
		this.gsonEncoder = encoder;
		initObjectWriters();
		initTypedWriters();
	}

	@Override
	public void jacksonSmileDecoder(Decoder<?> decoder) {
		this.jacksonSmileDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void jacksonSmileEncoder(Encoder<?> encoder) {
		this.jacksonSmileEncoder = encoder;
		initObjectWriters();
		initTypedWriters();
	}

	@Override
	public void jacksonCborDecoder(Decoder<?> decoder) {
		this.jacksonCborDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void jacksonCborEncoder(Encoder<?> encoder) {
		this.jacksonCborEncoder = encoder;
		initObjectWriters();
		initTypedWriters();
	}

	@Override
	public void protobufDecoder(Decoder<?> decoder) {
		this.protobufDecoder = decoder;
		initTypedReaders();
	}

	@Override
	public void protobufEncoder(Encoder<?> encoder) {
		this.protobufEncoder = encoder;
		initTypedWriters();
	}

	@Override
	public void jaxb2Decoder(Decoder<?> decoder) {
		this.jaxb2Decoder = decoder;
		initObjectReaders();
	}

	@Override
	public void jaxb2Encoder(Encoder<?> encoder) {
		this.jaxb2Encoder = encoder;
		initObjectWriters();
	}

	@Override
	public void kotlinSerializationCborDecoder(Decoder<?> decoder) {
		this.kotlinSerializationCborDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void kotlinSerializationCborEncoder(Encoder<?> encoder) {
		this.kotlinSerializationCborEncoder = encoder;
		initObjectWriters();
	}

	@Override
	public void kotlinSerializationJsonDecoder(Decoder<?> decoder) {
		this.kotlinSerializationJsonDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void kotlinSerializationJsonEncoder(Encoder<?> encoder) {
		this.kotlinSerializationJsonEncoder = encoder;
		initObjectWriters();
	}

	@Override
	public void kotlinSerializationProtobufDecoder(Decoder<?> decoder) {
		this.kotlinSerializationProtobufDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void kotlinSerializationProtobufEncoder(Encoder<?> encoder) {
		this.kotlinSerializationProtobufEncoder = encoder;
		initObjectWriters();
	}

	@Override
	public void configureDefaultCodec(Consumer<Object> codecConsumer) {
		this.codecConsumer = (this.codecConsumer != null ?
				this.codecConsumer.andThen(codecConsumer) : codecConsumer);
		initReaders();
		initWriters();
	}

	@Override
	public void maxInMemorySize(int byteCount) {
		if (!ObjectUtils.nullSafeEquals(this.maxInMemorySize, byteCount)) {
			this.maxInMemorySize = byteCount;
			initReaders();
		}
	}

	@Override
	public @Nullable Integer maxInMemorySize() {
		return this.maxInMemorySize;
	}

	@Override
	public void enableLoggingRequestDetails(boolean enable) {
		if (!ObjectUtils.nullSafeEquals(this.enableLoggingRequestDetails, enable)) {
			this.enableLoggingRequestDetails = enable;
			initReaders();
			initWriters();
		}
	}

	@Override
	public CodecConfigurer.MultipartCodecs multipartCodecs() {
		if (this.multipartCodecs == null) {
			this.multipartCodecs = new DefaultMultipartCodecs();
		}
		return this.multipartCodecs;
	}

	@Override
	public void multipartReader(HttpMessageReader<?> multipartReader) {
		this.multipartReader = multipartReader;
		initTypedReaders();
	}

	/**
	 * Set a supplier for part writers to use when
	 * {@link #multipartCodecs()} are not explicitly configured.
	 * That's the same set of writers as for general except for the multipart
	 * writer itself.
	 */
	void setPartWritersSupplier(Supplier<List<HttpMessageWriter<?>>> supplier) {
		this.partWritersSupplier = supplier;
		initTypedWriters();
	}

	@Override
	public @Nullable Boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

	/**
	 * Delegate method used from {@link BaseCodecConfigurer#registerDefaults}.
	 */
	void registerDefaults(boolean registerDefaults) {
		if (this.registerDefaults != registerDefaults) {
			this.registerDefaults = registerDefaults;
			initReaders();
			initWriters();
		}
	}


	/**
	 * Return readers that support specific types.
	 */
	final List<HttpMessageReader<?>> getTypedReaders() {
		return this.typedReaders;
	}

	/**
	 * Reset and initialize typed readers.
	 * @since 5.3.3
	 */
	protected void initTypedReaders() {
		this.typedReaders.clear();
		if (!this.registerDefaults) {
			return;
		}
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new DataBufferDecoder()));
		if (NETTY_BYTE_BUF_PRESENT) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new NettyByteBufDecoder()));
		}
		addCodec(this.typedReaders, new ResourceHttpMessageReader(new ResourceDecoder()));
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
		if (PROTOBUF_PRESENT) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(this.protobufDecoder != null ?
					(ProtobufDecoder) this.protobufDecoder : new ProtobufDecoder()));
		}
		addCodec(this.typedReaders, new FormHttpMessageReader());
		if (this.multipartReader != null) {
			addCodec(this.typedReaders, this.multipartReader);
		}
		else {
			DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
			addCodec(this.typedReaders, partReader);
			addCodec(this.typedReaders, new MultipartHttpMessageReader(partReader));
		}
		addCodec(this.typedReaders, new PartEventHttpMessageReader());

		// client vs server..
		extendTypedReaders(this.typedReaders);
	}

	/**
	 * Initialize a codec and add it to the List.
	 * @since 5.1.13
	 */
	protected <T> void addCodec(List<T> codecs, T codec) {
		initCodec(codec);
		codecs.add(codec);
	}

	/**
	 * Apply {@link #maxInMemorySize()} and {@link #enableLoggingRequestDetails},
	 * if configured by the application, to the given codec , including any
	 * codec it contains.
	 */
	@SuppressWarnings("removal")
	private void initCodec(@Nullable Object codec) {
		if (codec instanceof DecoderHttpMessageReader<?> decoderHttpMessageReader) {
			codec = decoderHttpMessageReader.getDecoder();
		}
		else if (codec instanceof EncoderHttpMessageWriter<?> encoderHttpMessageWriter) {
			codec = encoderHttpMessageWriter.getEncoder();
		}

		if (codec == null) {
			return;
		}

		Integer size = this.maxInMemorySize;
		if (size != null) {
			if (codec instanceof AbstractDataBufferDecoder<?> abstractDataBufferDecoder) {
				abstractDataBufferDecoder.setMaxInMemorySize(size);
			}
			// Pattern variables in the following if-blocks cannot be named the same as instance fields
			// due to lacking support in Checkstyle: https://github.com/checkstyle/checkstyle/issues/10969
			if (PROTOBUF_PRESENT) {
				if (codec instanceof ProtobufDecoder protobufDec) {
					protobufDec.setMaxMessageSize(size);
				}
			}
			if (KOTLIN_SERIALIZATION_CBOR_PRESENT) {
				if (codec instanceof KotlinSerializationCborDecoder kotlinSerializationCborDec) {
					kotlinSerializationCborDec.setMaxInMemorySize(size);
				}
			}
			if (KOTLIN_SERIALIZATION_JSON_PRESENT) {
				if (codec instanceof KotlinSerializationJsonDecoder kotlinSerializationJsonDec) {
					kotlinSerializationJsonDec.setMaxInMemorySize(size);
				}
			}
			if (KOTLIN_SERIALIZATION_PROTOBUF_PRESENT) {
				if (codec instanceof KotlinSerializationProtobufDecoder kotlinSerializationProtobufDec) {
					kotlinSerializationProtobufDec.setMaxInMemorySize(size);
				}
			}
			if (JACKSON_PRESENT) {
				if (codec instanceof AbstractJacksonDecoder<?> abstractJacksonDecoder) {
					abstractJacksonDecoder.setMaxInMemorySize(size);
				}
			}
			if (JACKSON_2_PRESENT) {
				if (codec instanceof AbstractJackson2Decoder abstractJackson2Decoder) {
					abstractJackson2Decoder.setMaxInMemorySize(size);
				}
			}
			if (JAXB_2_PRESENT) {
				if (codec instanceof Jaxb2XmlDecoder jaxb2XmlDecoder) {
					jaxb2XmlDecoder.setMaxInMemorySize(size);
				}
			}
			if (codec instanceof FormHttpMessageReader formHttpMessageReader) {
				formHttpMessageReader.setMaxInMemorySize(size);
			}
			if (codec instanceof ServerSentEventHttpMessageReader serverSentEventHttpMessageReader) {
				serverSentEventHttpMessageReader.setMaxInMemorySize(size);
			}
			if (codec instanceof DefaultPartHttpMessageReader defaultPartHttpMessageReader) {
				defaultPartHttpMessageReader.setMaxInMemorySize(size);
			}
			if (codec instanceof PartEventHttpMessageReader partEventHttpMessageReader) {
				partEventHttpMessageReader.setMaxInMemorySize(size);
			}
		}

		Boolean enable = this.enableLoggingRequestDetails;
		if (enable != null) {
			if (codec instanceof FormHttpMessageReader formHttpMessageReader) {
				formHttpMessageReader.setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof MultipartHttpMessageReader multipartHttpMessageReader) {
				multipartHttpMessageReader.setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof DefaultPartHttpMessageReader defaultPartHttpMessageReader) {
				defaultPartHttpMessageReader.setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof PartEventHttpMessageReader partEventHttpMessageReader) {
				partEventHttpMessageReader.setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof FormHttpMessageWriter formHttpMessageWriter) {
				formHttpMessageWriter.setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof MultipartHttpMessageWriter multipartHttpMessageWriter) {
				multipartHttpMessageWriter.setEnableLoggingRequestDetails(enable);
			}
		}

		if (this.codecConsumer != null) {
			this.codecConsumer.accept(codec);
		}

		// Recurse for nested codecs
		if (codec instanceof MultipartHttpMessageReader multipartHttpMessageReader) {
			initCodec(multipartHttpMessageReader.getPartReader());
		}
		else if (codec instanceof MultipartHttpMessageWriter multipartHttpMessageWriter) {
			initCodec(multipartHttpMessageWriter.getFormWriter());
		}
		else if (codec instanceof ServerSentEventHttpMessageReader serverSentEventHttpMessageReader) {
			initCodec(serverSentEventHttpMessageReader.getDecoder());
		}
		else if (codec instanceof ServerSentEventHttpMessageWriter serverSentEventHttpMessageWriter) {
			initCodec(serverSentEventHttpMessageWriter.getEncoder());
		}
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
		return this.objectReaders;
	}

	/**
	 * Reset and initialize object readers.
	 * @since 5.3.3
	 */
	protected void initObjectReaders() {
		this.objectReaders.clear();
		if (!this.registerDefaults) {
			return;
		}
		if (KOTLIN_SERIALIZATION_JSON_PRESENT) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getKotlinSerializationJsonDecoder()));
		}
		if (JACKSON_PRESENT || JACKSON_2_PRESENT) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJacksonJsonDecoder()));
		}
		else if (GSON_PRESENT) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getGsonDecoder()));
		}
		if (JACKSON_SMILE_PRESENT || JACKSON_2_SMILE_PRESENT) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJacksonSmileDecoder()));
		}
		if (KOTLIN_SERIALIZATION_CBOR_PRESENT) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getKotlinSerializationCborDecoder()));
		}
		if (JACKSON_CBOR_PRESENT || JACKSON_2_CBOR_PRESENT) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJacksonCborDecoder()));
		}
		if (JAXB_2_PRESENT) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(this.jaxb2Decoder != null ?
					(Jaxb2XmlDecoder) this.jaxb2Decoder : new Jaxb2XmlDecoder()));
		}
		if (KOTLIN_SERIALIZATION_PROTOBUF_PRESENT) {
			addCodec(this.objectReaders,
					new DecoderHttpMessageReader<>(getKotlinSerializationProtobufDecoder()));
		}

		// client vs server..
		extendObjectReaders(this.objectReaders);
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
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		addCodec(readers, new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		return readers;
	}

	/**
	 * Return all writers that support specific types.
	 */
	final List<HttpMessageWriter<?>> getTypedWriters() {
		return this.typedWriters;
	}

	/**
	 * Reset and initialize typed writers.
	 * @since 5.3.3
	 */
	protected void initTypedWriters() {
		this.typedWriters.clear();
		if (!this.registerDefaults) {
			return;
		}
		this.typedWriters.addAll(getBaseTypedWriters());
		extendTypedWriters(this.typedWriters);
	}

	/**
	 * Return "base" typed writers only, i.e. common to client and server.
	 */
	final List<HttpMessageWriter<?>> getBaseTypedWriters() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		addCodec(writers, new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
		addCodec(writers, new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		addCodec(writers, new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
		if (NETTY_BYTE_BUF_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(new NettyByteBufEncoder()));
		}
		addCodec(writers, new ResourceHttpMessageWriter());
		addCodec(writers, new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		if (PROTOBUF_PRESENT) {
			addCodec(writers, new ProtobufHttpMessageWriter(this.protobufEncoder != null ?
					(ProtobufEncoder) this.protobufEncoder : new ProtobufEncoder()));
		}
		addCodec(writers, new MultipartHttpMessageWriter(this::getPartWriters, new FormHttpMessageWriter()));
		addCodec(writers, new PartEventHttpMessageWriter());
		addCodec(writers, new PartHttpMessageWriter());
		return writers;
	}

	private List<HttpMessageWriter<?>> getPartWriters() {
		if (this.multipartCodecs != null) {
			return this.multipartCodecs.getWriters();
		}
		else if (this.partWritersSupplier != null) {
			return this.partWritersSupplier.get();
		}
		else {
			return Collections.emptyList();
		}
	}


	/**
	 * Hook for client or server specific typed writers.
	 */
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
	}

	/**
	 * Return Object writers (JSON, XML, SSE).
	 */
	final List<HttpMessageWriter<?>> getObjectWriters() {
		return this.objectWriters;
	}

	/**
	 * Reset and initialize object writers.
	 * @since 5.3.3
	 */
	protected void initObjectWriters() {
		this.objectWriters.clear();
		if (!this.registerDefaults) {
			return;
		}
		this.objectWriters.addAll(getBaseObjectWriters());
		extendObjectWriters(this.objectWriters);
	}

	/**
	 * Return "base" object writers only, i.e. common to client and server.
	 */
	final List<HttpMessageWriter<?>> getBaseObjectWriters() {
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		if (KOTLIN_SERIALIZATION_JSON_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getKotlinSerializationJsonEncoder()));
		}
		if (JACKSON_PRESENT || JACKSON_2_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getJacksonJsonEncoder()));
		}
		else if (GSON_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getGsonEncoder()));
		}
		if (JACKSON_SMILE_PRESENT || JACKSON_2_SMILE_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getJacksonSmileEncoder()));
		}
		if (KOTLIN_SERIALIZATION_CBOR_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getKotlinSerializationCborEncoder()));
		}
		if (JACKSON_CBOR_PRESENT || JACKSON_2_CBOR_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getJacksonCborEncoder()));
		}
		if (JAXB_2_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(this.jaxb2Encoder != null ?
					(Jaxb2XmlEncoder) this.jaxb2Encoder : new Jaxb2XmlEncoder()));
		}
		if (KOTLIN_SERIALIZATION_PROTOBUF_PRESENT) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getKotlinSerializationProtobufEncoder()));
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

	void applyDefaultConfig(BaseCodecConfigurer.DefaultCustomCodecs customCodecs) {
		applyDefaultConfig(customCodecs.getTypedReaders());
		applyDefaultConfig(customCodecs.getObjectReaders());
		applyDefaultConfig(customCodecs.getTypedWriters());
		applyDefaultConfig(customCodecs.getObjectWriters());
		customCodecs.getDefaultConfigConsumers().forEach(consumer -> consumer.accept(this));
	}

	private void applyDefaultConfig(Map<?, Boolean> readers) {
		readers.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.forEach(this::initCodec);
	}


	// Accessors for use in subclasses...

	@SuppressWarnings("removal")
	protected Decoder<?> getJacksonJsonDecoder() {
		if (this.jacksonJsonDecoder == null) {
			if (JACKSON_PRESENT) {
				this.jacksonJsonDecoder = new JacksonJsonDecoder();
			}
			else if (JACKSON_2_PRESENT) {
				this.jacksonJsonDecoder = new Jackson2JsonDecoder();
			}
			else {
				throw new IllegalStateException("Jackson not present");
			}
		}
		return this.jacksonJsonDecoder;
	}

	/**
	 * Get or initialize a Jackson JSON decoder.
	 * @deprecated in favor of {@link #getJacksonJsonDecoder()}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	protected Decoder<?> getJackson2JsonDecoder() {
		return getJacksonJsonDecoder();
	}

	@SuppressWarnings("removal")
	protected Encoder<?> getJacksonJsonEncoder() {
		if (this.jacksonJsonEncoder == null) {
			if (JACKSON_PRESENT) {
				this.jacksonJsonEncoder = new JacksonJsonEncoder();
			}
			else if (JACKSON_2_PRESENT) {
				this.jacksonJsonEncoder = new Jackson2JsonEncoder();
			}
			else {
				throw new IllegalStateException("Jackson not present");
			}
		}
		return this.jacksonJsonEncoder;
	}

	/**
	 * Get or initialize a Jackson JSON encoder.
	 * @deprecated in favor of {@link #getJacksonJsonEncoder()}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	protected Encoder<?> getJackson2JsonEncoder() {
		return getJacksonJsonEncoder();
	}

	protected Decoder<?> getGsonDecoder() {
		if (this.gsonDecoder == null) {
			this.gsonDecoder = new GsonDecoder();
		}
		return this.gsonDecoder;
	}

	protected Encoder<?> getGsonEncoder() {
		if (this.gsonEncoder == null) {
			this.gsonEncoder = new GsonEncoder();
		}
		return this.gsonEncoder;
	}

	@SuppressWarnings("removal")
	protected Decoder<?> getJacksonSmileDecoder() {
		if (this.jacksonSmileDecoder == null) {
			if (JACKSON_SMILE_PRESENT) {
				this.jacksonSmileDecoder = new JacksonSmileDecoder();
			}
			else if (JACKSON_2_SMILE_PRESENT) {
				this.jacksonSmileDecoder = new Jackson2SmileDecoder();
			}
			else {
				throw new IllegalStateException("Jackson Smile support not present");
			}
		}
		return this.jacksonSmileDecoder;
	}

	@SuppressWarnings("removal")
	protected Encoder<?> getJacksonSmileEncoder() {
		if (this.jacksonSmileEncoder == null) {
			if (JACKSON_SMILE_PRESENT) {
				this.jacksonSmileEncoder = new JacksonSmileEncoder();
			}
			else if (JACKSON_2_SMILE_PRESENT) {
				this.jacksonSmileEncoder = new Jackson2SmileEncoder();
			}
			else {
				throw new IllegalStateException("Jackson Smile support not present");
			}
		}
		return this.jacksonSmileEncoder;
	}

	@SuppressWarnings("removal")
	protected Decoder<?> getJacksonCborDecoder() {
		if (this.jacksonCborDecoder == null) {
			if (JACKSON_CBOR_PRESENT) {
				this.jacksonCborDecoder = new JacksonCborDecoder();
			}
			else if (JACKSON_2_CBOR_PRESENT) {
				this.jacksonCborDecoder = new Jackson2CborDecoder();
			}
			else {
				throw new IllegalStateException("Jackson CBOR support not present");
			}
		}
		return this.jacksonCborDecoder;
	}

	@SuppressWarnings("removal")
	protected Encoder<?> getJacksonCborEncoder() {
		if (this.jacksonCborEncoder == null) {
			if (JACKSON_CBOR_PRESENT) {
				this.jacksonCborEncoder = new JacksonCborEncoder();
			}
			else if (JACKSON_2_CBOR_PRESENT) {
				this.jacksonCborEncoder = new Jackson2CborEncoder();
			}
			else {
				throw new IllegalStateException("Jackson CBOR support not present");
			}
		}
		return this.jacksonCborEncoder;
	}

	protected Decoder<?> getKotlinSerializationJsonDecoder() {
		if (this.kotlinSerializationJsonDecoder == null) {
			this.kotlinSerializationJsonDecoder = (this.jacksonJsonDecoder != null || JACKSON_PRESENT || JACKSON_2_PRESENT || GSON_PRESENT ?
					new KotlinSerializationJsonDecoder() : new KotlinSerializationJsonDecoder(type -> true));
		}
		return this.kotlinSerializationJsonDecoder;
	}

	protected Encoder<?> getKotlinSerializationJsonEncoder() {
		if (this.kotlinSerializationJsonEncoder == null) {
			this.kotlinSerializationJsonEncoder = (this.jacksonJsonDecoder != null || JACKSON_PRESENT || JACKSON_2_PRESENT || GSON_PRESENT ?
					new KotlinSerializationJsonEncoder() : new KotlinSerializationJsonEncoder(type -> true));
		}
		return this.kotlinSerializationJsonEncoder;
	}

	protected Decoder<?> getKotlinSerializationCborDecoder() {
		if (this.kotlinSerializationCborDecoder == null) {
			this.kotlinSerializationCborDecoder = (this.jacksonCborDecoder != null || JACKSON_CBOR_PRESENT ?
					new KotlinSerializationCborDecoder() : new KotlinSerializationCborDecoder(type -> true));
		}
		return this.kotlinSerializationCborDecoder;
	}

	protected Encoder<?> getKotlinSerializationCborEncoder() {
		if (this.kotlinSerializationCborEncoder == null) {
			this.kotlinSerializationCborEncoder = (this.jacksonCborDecoder != null || JACKSON_CBOR_PRESENT ?
					new KotlinSerializationCborEncoder() : new KotlinSerializationCborEncoder(type -> true));
		}
		return this.kotlinSerializationCborEncoder;
	}

	protected Decoder<?> getKotlinSerializationProtobufDecoder() {
		if (this.kotlinSerializationProtobufDecoder == null) {
			this.kotlinSerializationProtobufDecoder = new KotlinSerializationProtobufDecoder(type -> true);
		}
		return this.kotlinSerializationProtobufDecoder;
	}

	protected Encoder<?> getKotlinSerializationProtobufEncoder() {
		if (this.kotlinSerializationProtobufEncoder == null) {
			this.kotlinSerializationProtobufEncoder = new KotlinSerializationProtobufEncoder(type -> true);
		}
		return this.kotlinSerializationProtobufEncoder;
	}


	/**
	 * Default implementation of {@link CodecConfigurer.MultipartCodecs}.
	 */
	protected class DefaultMultipartCodecs implements CodecConfigurer.MultipartCodecs {

		private final List<HttpMessageWriter<?>> writers = new ArrayList<>();


		DefaultMultipartCodecs() {
		}

		DefaultMultipartCodecs(DefaultMultipartCodecs other) {
			this.writers.addAll(other.writers);
		}


		@Override
		public CodecConfigurer.MultipartCodecs encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
			initTypedWriters();
			return this;
		}

		@Override
		public CodecConfigurer.MultipartCodecs writer(HttpMessageWriter<?> writer) {
			this.writers.add(writer);
			initTypedWriters();
			return this;
		}

		List<HttpMessageWriter<?>> getWriters() {
			return this.writers;
		}
	}


}
