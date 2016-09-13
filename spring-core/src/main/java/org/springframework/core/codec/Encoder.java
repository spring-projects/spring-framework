/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.codec;

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.MimeType;

/**
 * Strategy to encode a stream of Objects of type {@code <T>} into an output
 * stream of bytes.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the input stream
 */
public interface Encoder<T> {

	/**
	 * Whether the encoder supports the given source element type and the MIME
	 * type for the output stream.
	 * @param elementType the type of elements in the source stream
	 * @param mimeType the MIME type for the output stream
	 * @param hints additional information about how to do encode
	 * @return {@code true} if supported, {@code false} otherwise
	 */
	boolean canEncode(ResolvableType elementType, MimeType mimeType, Map<String, Object> hints);

	/**
	 * Encode a stream of Objects of type {@code T} into a {@link DataBuffer}
	 * output stream.
	 * @param inputStream the input stream of Objects to encode. If the input should be
	 * encoded as a single value rather than as a stream of elements, an instance of
	 * {@link Mono} should be used.
	 * @param bufferFactory for creating output stream {@code DataBuffer}'s
	 * @param elementType the expected type of elements in the input stream;
	 * this type must have been previously passed to the {@link #canEncode}
	 * method and it must have returned {@code true}.
	 * @param mimeType the MIME type for the output stream
	 * @param hints additional information about how to do encode
	 * @return the output stream
	 */
	Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, MimeType mimeType, Map<String, Object> hints);

	/**
	 * Return the list of mime types this encoder supports.
	 */
	List<MimeType> getEncodableMimeTypes();

}
