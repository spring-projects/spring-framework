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

package org.springframework.http.codec.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.SpringProperties;
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
import org.springframework.http.codec.json.AbstractJackson2Decoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
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
class BaseDefaultCodecs implements CodecConfigurer.DefaultCodecs, CodecConfigurer.DefaultCodecConfig {

	/**
	 * Boolean flag controlled by a {@code spring.xml.ignore} system property that instructs Spring to
	 * ignore XML, i.e. to not initialize the XML-related infrastructure.
	 * <p>The default is "false".
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	static final boolean jackson2Present;

	private static final boolean jackson2SmilePresent;

	private static final boolean jaxb2Present;

	private static final boolean protobufPresent;

	static final boolean synchronossMultipartPresent;

	static final boolean nettyByteBufPresent;

	static {
		ClassLoader classLoader = BaseCodecConfigurer.class.getClassLoader();
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		protobufPresent = ClassUtils.isPresent("com.google.protobuf.Message", classLoader);
		synchronossMultipartPresent = ClassUtils.isPresent("org.synchronoss.cloud.nio.multipart.NioMultipartParser", classLoader);
		nettyByteBufPresent = ClassUtils.isPresent("io.netty.buffer.ByteBuf", classLoader);
	}


	@Nullable
	private Decoder<?> jackson2JsonDecoder;

	@Nullable
	private Encoder<?> jackson2JsonEncoder;

	@Nullable
	private Encoder<?> jackson2SmileEncoder;

	@Nullable
	private Decoder<?> jackson2SmileDecoder;

	@Nullable
	private Decoder<?> protobufDecoder;

	@Nullable
	private Encoder<?> protobufEncoder;

	@Nullable
	private Decoder<?> jaxb2Decoder;

	@Nullable
	private Encoder<?> jaxb2Encoder;

	@Nullable
	private Integer maxInMemorySize;

	@Nullable
	private Boolean enableLoggingRequestDetails;

	private boolean registerDefaults = true;


	BaseDefaultCodecs() {
	}

	/**
	 * Create a deep copy of the given {@link BaseDefaultCodecs}.
	 */
	protected BaseDefaultCodecs(BaseDefaultCodecs other) {
		this.jackson2JsonDecoder = other.jackson2JsonDecoder;
		this.jackson2JsonEncoder = other.jackson2JsonEncoder;
		this.jackson2SmileDecoder = other.jackson2SmileDecoder;
		this.jackson2SmileEncoder = other.jackson2SmileEncoder;
		this.protobufDecoder = other.protobufDecoder;
		this.protobufEncoder = other.protobufEncoder;
		this.jaxb2Decoder = other.jaxb2Decoder;
		this.jaxb2Encoder = other.jaxb2Encoder;
		this.maxInMemorySize = other.maxInMemorySize;
		this.enableLoggingRequestDetails = other.enableLoggingRequestDetails;
		this.registerDefaults = other.registerDefaults;
	}

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
	public void jackson2SmileDecoder(Decoder<?> decoder) {
		this.jackson2SmileDecoder = decoder;
	}

	@Override
	public void jackson2SmileEncoder(Encoder<?> encoder) {
		this.jackson2SmileEncoder = encoder;
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
	public void maxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	@Override
	@Nullable
	public Integer maxInMemorySize() {
		return this.maxInMemorySize;
	}

	@Override
	public void enableLoggingRequestDetails(boolean enable) {
		this.enableLoggingRequestDetails = enable;
	}

	@Override
	@Nullable
	public Boolean isEnableLoggingRequestDetails() {
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
		addCodec(readers, new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		addCodec(readers, new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		addCodec(readers, new DecoderHttpMessageReader<>(new DataBufferDecoder()));
		if (nettyByteBufPresent) {
			addCodec(readers, new DecoderHttpMessageReader<>(new NettyByteBufDecoder()));
		}
		addCodec(readers, new ResourceHttpMessageReader(new ResourceDecoder()));
		addCodec(readers, new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
		if (protobufPresent) {
			addCodec(readers, new DecoderHttpMessageReader<>(this.protobufDecoder != null ?
					(ProtobufDecoder) this.protobufDecoder : new ProtobufDecoder()));
		}
		addCodec(readers, new FormHttpMessageReader());

		// client vs server..
		extendTypedReaders(readers);

		return readers;
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
	@SuppressWarnings("rawtypes")
	private void initCodec(@Nullable Object codec) {

		if (codec instanceof DecoderHttpMessageReader) {
			codec = ((DecoderHttpMessageReader) codec).getDecoder();
		}

		if (codec == null) {
			return;
		}

		Integer size = this.maxInMemorySize;
		if (size != null) {
			if (codec instanceof AbstractDataBufferDecoder) {
				((AbstractDataBufferDecoder<?>) codec).setMaxInMemorySize(size);
			}
			if (protobufPresent) {
				if (codec instanceof ProtobufDecoder) {
					((ProtobufDecoder) codec).setMaxMessageSize(size);
				}
			}
			if (jackson2Present) {
				if (codec instanceof AbstractJackson2Decoder) {
					((AbstractJackson2Decoder) codec).setMaxInMemorySize(size);
				}
			}
			if (jaxb2Present && !shouldIgnoreXml) {
				if (codec instanceof Jaxb2XmlDecoder) {
					((Jaxb2XmlDecoder) codec).setMaxInMemorySize(size);
				}
			}
			if (codec instanceof FormHttpMessageReader) {
				((FormHttpMessageReader) codec).setMaxInMemorySize(size);
			}
			if (codec instanceof ServerSentEventHttpMessageReader) {
				((ServerSentEventHttpMessageReader) codec).setMaxInMemorySize(size);
				initCodec(((ServerSentEventHttpMessageReader) codec).getDecoder());
			}
			if (synchronossMultipartPresent) {
				if (codec instanceof SynchronossPartHttpMessageReader) {
					((SynchronossPartHttpMessageReader) codec).setMaxInMemorySize(size);
				}
			}
		}

		Boolean enable = this.enableLoggingRequestDetails;
		if (enable != null) {
			if (codec instanceof FormHttpMessageReader) {
				((FormHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof MultipartHttpMessageReader) {
				((MultipartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			if (synchronossMultipartPresent) {
				if (codec instanceof SynchronossPartHttpMessageReader) {
					((SynchronossPartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
				}
			}
			if (codec instanceof FormHttpMessageWriter) {
				((FormHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof MultipartHttpMessageWriter) {
				((MultipartHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
			}
		}

		if (codec instanceof MultipartHttpMessageReader) {
			initCodec(((MultipartHttpMessageReader) codec).getPartReader());
		}
		else if (codec instanceof MultipartHttpMessageWriter) {
			initCodec(((MultipartHttpMessageWriter) codec).getFormWriter());
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
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		if (jackson2Present) {
			addCodec(readers, new DecoderHttpMessageReader<>(getJackson2JsonDecoder()));
		}
		if (jackson2SmilePresent) {
			addCodec(readers, new DecoderHttpMessageReader<>(this.jackson2SmileDecoder != null ?
					(Jackson2SmileDecoder) this.jackson2SmileDecoder : new Jackson2SmileDecoder()));
		}
		if (jaxb2Present && !shouldIgnoreXml) {
			addCodec(readers, new DecoderHttpMessageReader<>(this.jaxb2Decoder != null ?
					(Jaxb2XmlDecoder) this.jaxb2Decoder : new Jaxb2XmlDecoder()));
		}

		// client vs server..
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
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		addCodec(readers, new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		return readers;
	}

	/**
	 * Return all writers that support specific types.
	 */
	@SuppressWarnings({"rawtypes" })
	final List<HttpMessageWriter<?>> getTypedWriters() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> writers = getBaseTypedWriters();
		extendTypedWriters(writers);
		return writers;
	}

	/**
	 * Return "base" typed writers only, i.e. common to client and server.
	 */
	@SuppressWarnings("unchecked")
	final List<HttpMessageWriter<?>> getBaseTypedWriters() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		writers.add(new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
		writers.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		writers.add(new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
		if (nettyByteBufPresent) {
			writers.add(new EncoderHttpMessageWriter<>(new NettyByteBufEncoder()));
		}
		writers.add(new ResourceHttpMessageWriter());
		writers.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		if (protobufPresent) {
			writers.add(new ProtobufHttpMessageWriter(this.protobufEncoder != null ?
					(ProtobufEncoder) this.protobufEncoder : new ProtobufEncoder()));
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
	 */
	final List<HttpMessageWriter<?>> getObjectWriters() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> writers = getBaseObjectWriters();
		extendObjectWriters(writers);
		return writers;
	}

	/**
	 * Return "base" object writers only, i.e. common to client and server.
	 */
	final List<HttpMessageWriter<?>> getBaseObjectWriters() {
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		if (jackson2Present) {
			writers.add(new EncoderHttpMessageWriter<>(getJackson2JsonEncoder()));
		}
		if (jackson2SmilePresent) {
			writers.add(new EncoderHttpMessageWriter<>(this.jackson2SmileEncoder != null ?
					(Jackson2SmileEncoder) this.jackson2SmileEncoder : new Jackson2SmileEncoder()));
		}
		if (jaxb2Present && !shouldIgnoreXml) {
			writers.add(new EncoderHttpMessageWriter<>(this.jaxb2Encoder != null ?
					(Jaxb2XmlEncoder) this.jaxb2Encoder : new Jaxb2XmlEncoder()));
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

	protected Decoder<?> getJackson2JsonDecoder() {
		if (this.jackson2JsonDecoder == null) {
			this.jackson2JsonDecoder = new Jackson2JsonDecoder();
		}
		return this.jackson2JsonDecoder;
	}

	protected Encoder<?> getJackson2JsonEncoder() {
		if (this.jackson2JsonEncoder == null) {
			this.jackson2JsonEncoder = new Jackson2JsonEncoder();
		}
		return this.jackson2JsonEncoder;
	}

}
