/*
 * Copyright 2002-2021 the original author or authors.
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

import java.lang.reflect.Type;
import java.util.Map;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.StringFormat;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * Decode a byte stream into JSON and convert to Objects with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This decoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports various Content Types with various character sets, {@code UTF-8} being the default.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 */
public class KotlinSerializationStringDecoder extends AbstractKotlinSerializationDecoder {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final StringFormat format;

	// String decoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
	private final StringDecoder stringDecoder = StringDecoder.allMimeTypes(StringDecoder.DEFAULT_DELIMITERS, false);


	public KotlinSerializationStringDecoder(final StringFormat format, final MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
		this.format = format;
	}

	/**
	 * Configure a limit on the number of bytes that can be buffered whenever
	 * the input stream needs to be aggregated. This can be a result of
	 * decoding to a single {@code DataBuffer},
	 * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
	 * {@link org.springframework.core.io.Resource Resource}, {@code String}, etc.
	 * It can also occur when splitting the input stream, e.g. delimited text,
	 * in which case the limit applies to data buffered between delimiters.
	 * <p>By default this is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 */
	@Override
	public void setMaxInMemorySize(final int byteCount) {
		this.stringDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 */
	@Override
	public int getMaxInMemorySize() {
		return this.stringDecoder.getMaxInMemorySize();
	}


	@Override
	public Mono<Object> decodeToMono(final Publisher<DataBuffer> inputStream, final ResolvableType elementType,
										@Nullable final MimeType mimeType, @Nullable final Map<String, Object> hints) {

		return this.stringDecoder
				.decodeToMono(inputStream, elementType, mimeType, hints)
				.map(string -> this.format.decodeFromString(serializer(elementType.getType()), string));
	}

}
