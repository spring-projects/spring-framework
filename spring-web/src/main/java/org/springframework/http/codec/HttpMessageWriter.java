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

package org.springframework.http.codec;

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;

/**
 * Strategy interface that specifies a converter that can convert a stream of
 * Objects to a stream of bytes to be written to the HTTP response body.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HttpMessageWriter<T> {

	/**
	 * Indicates whether the given class can be written by this converter.
	 * @param elementType the stream element type to test for writability
	 * @param mediaType the media type to write, can be {@code null} if not specified.
	 * Typically the value of an {@code Accept} header.
	 * @param hints additional information about how to write
	 * @return {@code true} if writable; {@code false} otherwise
	 */
	boolean canWrite(ResolvableType elementType, MediaType mediaType, Map<String, Object> hints);

	/**
	 * Write an given object to the given output message.
	 * @param inputStream the input stream to write
	 * @param elementType the stream element type to process. This type must have previously
	 * been passed to the {@link #canWrite} canWrite} method of this interface, which must
	 * have returned {@code true}.
	 * @param mediaType the content type to use when writing, typically the value of an
	 * {@code Accept} header. May be {@code null} to indicate that the default content
	 * type of the converter must be used.
	 * @param outputMessage the message to write to
	 * @param hints additional information about how to write
	 * @return the converted {@link Mono} of object
	 */
	Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			MediaType mediaType, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints);

	/**
	 * Return the list of {@link MediaType} objects that can be written by this converter.
	 * @return the list of supported readable media types
	 */
	List<MediaType> getWritableMediaTypes();

}
