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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.StringFormat;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link Encoder} implementations that defer to Kotlin
 * {@linkplain StringFormat string serializers}.
 *
 * <p>As of Spring Framework 7.0,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphism</a>
 * is supported.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @author Arjen Poutsma
 * @since 6.0
 * @param <T> the type of {@link StringFormat}
 */
public abstract class KotlinSerializationStringEncoder<T extends StringFormat> extends KotlinSerializationSupport<T>
	implements Encoder<Object> {

	private static final byte[] NEWLINE_SEPARATOR = {'\n'};

	protected static final byte[] EMPTY_BYTES = new byte[0];


	// CharSequence encoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
	private final CharSequenceEncoder charSequenceEncoder = CharSequenceEncoder.allMimeTypes();
	private final Set<MimeType> streamingMediaTypes = new HashSet<>();

	protected KotlinSerializationStringEncoder(T format, MimeType... supportedMimeTypes) {
		super(format, supportedMimeTypes);
	}

	/**
	 * Set streaming {@link MediaType MediaTypes}.
	 * @param streamingMediaTypes streaming {@link MediaType MediaTypes}
	 * @since 6.1.4
	 */
	public void setStreamingMediaTypes(Collection<MediaType> streamingMediaTypes) {
		this.streamingMediaTypes.clear();
		this.streamingMediaTypes.addAll(streamingMediaTypes);
	}

	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return canSerialize(elementType, mimeType);
	}

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return supportedMimeTypes();
	}

	@Override
	public List<MimeType> getEncodableMimeTypes(ResolvableType elementType) {
		return supportedMimeTypes();
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (inputStream instanceof Mono<?> mono) {
			return mono
					.map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
					.flux();
		}
		if (mimeType != null && this.streamingMediaTypes.contains(mimeType)) {
			return Flux.from(inputStream)
					.map(value -> encodeStreamingValue(value, bufferFactory, elementType, mimeType, hints, EMPTY_BYTES,
							NEWLINE_SEPARATOR));
		}
		return encodeNonStream(inputStream, bufferFactory, elementType, mimeType, hints);
	}

	protected DataBuffer encodeStreamingValue(Object value, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints, byte[] prefix, byte[] suffix) {

		List<DataBuffer> buffers = new ArrayList<>(3);
		if (prefix.length > 0) {
			buffers.add(bufferFactory.allocateBuffer(prefix.length).write(prefix));
		}
		buffers.add(encodeValue(value, bufferFactory, valueType, mimeType, hints));
		if (suffix.length > 0) {
			buffers.add(bufferFactory.allocateBuffer(suffix.length).write(suffix));
		}
		return bufferFactory.join(buffers);
	}

	protected Flux<DataBuffer> encodeNonStream(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
		return Flux.from(inputStream)
				.collectList()
				.map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints))
				.flux();
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		KSerializer<Object> serializer = serializer(valueType);
		if (serializer == null) {
			throw new EncodingException("Could not find KSerializer for " + valueType);
		}
		String string = format().encodeToString(serializer, value);
		return this.charSequenceEncoder.encodeValue(string, bufferFactory, valueType, mimeType, null);
	}
}
