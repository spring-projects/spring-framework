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

package org.springframework.http.converter.reactive;

import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ReactiveHttpOutputMessage;

/**
 * Strategy interface that specifies a converter that can convert from and to HTTP
 * requests and responses.
 * @author Arjen Poutsma
 */
public interface HttpMessageConverter<T> {

	/**
	 * Indicates whether the given class can be read by this converter.
	 * @param type the type to test for readability
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically the value of a {@code Content-Type} header.
	 * @return {@code true} if readable; {@code false} otherwise
	 */
	boolean canRead(ResolvableType type, MediaType mediaType);

	/**
	 * Return the list of {@link MediaType} objects that can be read by this converter.
	 * @return the list of supported readable media types
	 */
	List<MediaType> getReadableMediaTypes();

	/**
	 * Read an object of the given type form the given input message, and returns it.
	 * @param type the type of object to return. This type must have previously been
	 * passed to the
	 * {@link #canRead canRead} method of this interface, which must have returned {@code
	 * true}.
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 */
	Flux<T> read(ResolvableType type, ReactiveHttpInputMessage inputMessage);

	/**
	 * Indicates whether the given class can be written by this converter.
	 * @param type the class to test for writability
	 * @param mediaType the media type to write, can be {@code null} if not specified.
	 * Typically the value of an {@code Accept} header.
	 * @return {@code true} if writable; {@code false} otherwise
	 */
	boolean canWrite(ResolvableType type, MediaType mediaType);

	/**
	 * Return the list of {@link MediaType} objects that can be written by this
	 * converter.
	 * @return the list of supported readable media types
	 */
	List<MediaType> getWritableMediaTypes();

	/**
	 * Write an given object to the given output message.
	 * @param inputStream the input stream to write
	 * @param type the stream element type to process.
	 * @param contentType the content type to use when writing. May be {@code null} to
	 * indicate that the default content type of the converter must be used.
	 * @param outputMessage the message to write to
	 * @return
	 */
	Mono<Void> write(Publisher<? extends T> inputStream,
			ResolvableType type, MediaType contentType,
			ReactiveHttpOutputMessage outputMessage);
}