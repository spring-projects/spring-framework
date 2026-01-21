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

package org.springframework.http.codec;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import kotlinx.serialization.BinaryFormat;
import kotlinx.serialization.KSerializer;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link Decoder} implementations that defer to Kotlin
 * {@linkplain BinaryFormat binary serializers}.
 *
 * <p>As of Spring Framework 7.0, by default it only decodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics level
 * since it allows combined usage with other general purpose decoders without conflicts.
 * Alternative constructors with a {@code Predicate<ResolvableType>} parameter can be used
 * to customize this behavior.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @author Arjen Poutsma
 * @since 6.0
 * @param <T> the type of {@link BinaryFormat}
 */
public abstract class KotlinSerializationBinaryDecoder<T extends BinaryFormat> extends KotlinSerializationSupport<T>
	implements Decoder<Object> {

	// Byte array decoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
	private final ByteArrayDecoder byteArrayDecoder = new ByteArrayDecoder();


	/**
	 * Creates a new instance with the given format and supported mime types
	 * which only decodes types annotated with
	 * {@link kotlinx.serialization.Serializable @Serializable} at type or
	 * generics level.
	 */
	public KotlinSerializationBinaryDecoder(T format, MimeType... supportedMimeTypes) {
		super(format, supportedMimeTypes);
	}

	/**
	 * Creates a new instance with the given format and supported mime types
	 * which only decodes types for which the specified predicate returns
	 * {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationBinaryDecoder(T format, Predicate<ResolvableType> typePredicate, MimeType... supportedMimeTypes) {
		super(format, typePredicate, supportedMimeTypes);
	}

	/**
	 * Configure a limit on the number of bytes that can be buffered whenever
	 * the input stream needs to be aggregated. This can be a result of
	 * decoding to a single {@code DataBuffer},
	 * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
	 * {@link org.springframework.core.io.Resource Resource}, {@code String}, etc.
	 * It can also occur when splitting the input stream, for example, delimited text,
	 * in which case the limit applies to data buffered between delimiters.
	 * <p>By default this is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.byteArrayDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 */
	public int getMaxInMemorySize() {
		return this.byteArrayDecoder.getMaxInMemorySize();
	}

	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return canSerialize(elementType, mimeType);
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return supportedMimeTypes();
	}

	@Override
	public List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
		return supportedMimeTypes();
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		return Flux.error(new UnsupportedOperationException());
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
										@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		return Mono.defer(() -> {
			KSerializer<Object> serializer = serializer(elementType);
			if (serializer == null) {
				return Mono.error(new DecodingException("Could not find KSerializer for " + elementType));
			}
			return this.byteArrayDecoder
					.decodeToMono(inputStream, elementType, mimeType, hints)
					.handle((byteArray, sink) -> {
						try {
							sink.next(format().decodeFromByteArray(serializer, byteArray));
							sink.complete();
						}
						catch (IllegalArgumentException ex) {
							sink.error(new DecodingException("Decoding error: " + ex.getMessage(), ex));
						}
					});
		});
	}

}
