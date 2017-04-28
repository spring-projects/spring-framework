/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.File;
import java.util.Optional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;

/**
 * A representation of a part received in a multipart request. Could contain a file, the
 * string or json value of a parameter.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface Part {

	/**
	 * @return the headers of this part
	 */
	HttpHeaders getHeaders();

	/**
	 * @return the name of the parameter in the multipart form
	 */
	String getName();

	/**
	 * @return optionally the filename if the part contains a file
	 */
	Optional<String> getFilename();

	/**
	 * @return the content of the part as a String using the charset specified in the
	 * {@code Content-Type} header if any, or else using {@code UTF-8} by default.
	 */
	Mono<String> getContentAsString();

	/**
	 * @return the content of the part as a stream of bytes
	 */
	Flux<DataBuffer> getContent();

	/**
	 * Transfer the file contained in this part to the specified destination.
	 * @param dest the destination file
	 * @return a {@link Mono} that indicates completion of the file transfer or an error,
	 * for example an {@link IllegalStateException} if the part does not contain a file
	 */
	Mono<Void> transferTo(File dest);

}
