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

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.encoder.MessageToByteEncoder;

/**
 * Decode from a bytes stream to a message stream.
 *
 * @author Sebastien Deleuze
 * @see MessageToByteEncoder
 */
public interface ByteToMessageDecoder<T> {

	/**
	 * Indicate whether the given type and media type can be processed by this decoder.
	 * @param type the stream element type to ultimately decode to.
	 * @param mediaType the media type to decode from.
	 * Typically the value of a {@code Content-Type} header for HTTP request.
	 * @param hints Additional information about how to do decode, optional.
	 * @return {@code true} if decodable; {@code false} otherwise
	 */
	boolean canDecode(ResolvableType type, MediaType mediaType, Object... hints);

	/**
	 * Decode a bytes stream to a message stream.
	 * @param inputStream the input stream that represent the whole object to decode.
	 * @param type the stream element type to ultimately decode to.
	 * @param hints Additional information about how to do decode, optional.
	 * @return the decoded message stream
	 */
	Publisher<T> decode(Publisher<ByteBuffer> inputStream, ResolvableType type, MediaType mediaType, Object... hints);

}
