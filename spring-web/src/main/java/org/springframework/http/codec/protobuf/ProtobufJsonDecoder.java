/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.codec.protobuf;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * A {@code Decoder} that reads a JSON byte stream and converts it to
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>
 * {@link com.google.protobuf.Message}s.
 *
 * <p>Flux deserialized via
 * {@link #decode(Publisher, ResolvableType, MimeType, Map)} are not supported because
 * the Protobuf Java Util library does not provide a non-blocking parser
 * that splits a JSON stream into tokens.
 * Applications should consider decoding to {@code Mono<Message>} or
 * {@code Mono<List<Message>>}, which will use the supported
 * {@link #decodeToMono(Publisher, ResolvableType, MimeType, Map)}.
 *
 * <p>To generate {@code Message} Java classes, you need to install the
 * {@code protoc} binary.
 *
 * <p>This decoder requires Protobuf 3.29 or higher, and supports
 * {@code "application/json"} and {@code "application/*+json"} with
 * the official {@code "com.google.protobuf:protobuf-java-util"} library.
 *
 * @author Brian Clozel
 * @since 6.2
 * @see ProtobufJsonEncoder
 */
public class ProtobufJsonDecoder implements Decoder<Message> {

	/** The default max size for aggregating messages. */
	protected static final int DEFAULT_MESSAGE_MAX_SIZE = 256 * 1024;

	private static final List<MimeType> defaultMimeTypes = List.of(MediaType.APPLICATION_JSON,
			new MediaType("application", "*+json"));

	private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

	private final JsonFormat.Parser parser;

	private int maxMessageSize = DEFAULT_MESSAGE_MAX_SIZE;

	/**
	 * Construct a new {@link ProtobufJsonDecoder} using a default {@link JsonFormat.Parser} instance.
	 */
	public ProtobufJsonDecoder() {
		this(JsonFormat.parser());
	}

	/**
	 * Construct a new {@link ProtobufJsonDecoder} using the given {@link JsonFormat.Parser} instance.
	 */
	public ProtobufJsonDecoder(JsonFormat.Parser parser) {
		this.parser = parser;
	}

	/**
	 * Return the {@link #setMaxMessageSize configured} message size limit.
	 */
	public int getMaxMessageSize() {
		return this.maxMessageSize;
	}

	/**
	 * The max size allowed per message.
	 * <p>By default, this is set to 256K.
	 * @param maxMessageSize the max size per message, or -1 for unlimited
	 */
	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
	}

	private static boolean supportsMimeType(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			return false;
		}
		for (MimeType m : defaultMimeTypes) {
			if (m.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return defaultMimeTypes;
	}

	@Override
	public Flux<Message> decode(Publisher<DataBuffer> inputStream, ResolvableType targetType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		return Flux.error(new UnsupportedOperationException("Protobuf decoder does not support Flux, use Mono<List<...>> instead."));
	}

	@Override
	public Message decode(DataBuffer dataBuffer, ResolvableType targetType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {
		try {
			Message.Builder builder = getMessageBuilder(targetType.toClass());
			this.parser.merge(new InputStreamReader(dataBuffer.asInputStream()), builder);
			return builder.build();
		}
		catch (Exception ex) {
			throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
	}

	/**
	 * Create a new {@code Message.Builder} instance for the given class.
	 * <p>This method uses a ConcurrentHashMap for caching method lookups.
	 */
	private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
		Method method = methodCache.get(clazz);
		if (method == null) {
			method = clazz.getMethod("newBuilder");
			methodCache.put(clazz, method);
		}
		return (Message.Builder) method.invoke(clazz);
	}

	@Override
	public Mono<Message> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		return DataBufferUtils.join(inputStream, this.maxMessageSize)
				.map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints))
				.onErrorMap(DataBufferLimitException.class, exc -> new DecodingException("Could not decode JSON as Protobuf message", exc));
	}

}
