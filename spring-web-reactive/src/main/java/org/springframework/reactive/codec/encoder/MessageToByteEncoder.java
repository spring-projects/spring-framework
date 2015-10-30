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

package org.springframework.reactive.codec.encoder;

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.decoder.ByteToMessageDecoder;

/**
 * Encode from a message stream to a bytes stream.
 *
 * @author Sebastien Deleuze
 * @see ByteToMessageDecoder
 */
public interface MessageToByteEncoder<T> {

	/**
	 * Indicate whether the given type and media type can be processed by this encoder.
	 * @param type the stream element type to encode.
	 * @param mediaType the media type to encode.
	 * Typically the value of an {@code Accept} header for HTTP request.
	 * @param hints Additional information about how to encode, optional.
	 * @return {@code true} if encodable; {@code false} otherwise
	 */
	boolean canEncode(ResolvableType type, MediaType mediaType, Object... hints);

	/**
	 * Encode a given message stream to the given output byte stream.
	 * @param messageStream the message stream to encode.
	 * @param type the stream element type to encode.
	 * @param mediaType the media type to encode.
	 * Typically the value of an {@code Accept} header for HTTP request.
	 * @param hints Additional information about how to encode, optional.
	 * @return the encoded bytes stream
	 */
	Publisher<ByteBuffer> encode(Publisher<? extends T> messageStream, ResolvableType type,
			MediaType mediaType, Object... hints);

}
