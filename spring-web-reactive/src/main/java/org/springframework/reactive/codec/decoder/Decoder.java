/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.reactive.codec.decoder;

import java.nio.ByteBuffer;
import java.util.List;

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.reactive.codec.encoder.Encoder;
import org.springframework.util.MimeType;

/**
 * Decode from a bytes stream to a stream of {@code T}.
 *
 * @author Sebastien Deleuze
 * @see Encoder
 */
public interface Decoder<T> {

	/**
	 * Indicate whether the given type and mime type can be processed by this decoder.
	 * @param type the stream element type to process.
	 * @param mimeType the mime type to process.
	 * @param hints Additional information about how to do decode, optional.
	 * @return {@code true} if can process; {@code false} otherwise
	 */
	boolean canDecode(ResolvableType type, MimeType mimeType, Object... hints);

	/**
	 * Decode an input {@link ByteBuffer} stream to an output stream of {@code T}.
	 * @param inputStream the input stream to process.
	 * @param type the stream element type to process.
	 * @param mimeType the mime type to process.
	 * @param hints Additional information about how to do decode, optional.
	 * @return the output stream
	 */
	Publisher<T> decode(Publisher<ByteBuffer> inputStream, ResolvableType type, MimeType mimeType, Object... hints);

	/**
	 * Return the list of {@link MimeType} objects supported by this codec.
	 * @return the list of supported mime types
	 */
	List<MimeType> getSupportedMimeTypes();

}
