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
 * Representation for a part in a "multipart/form-data" request.
 *
 * <p>The origin of a multipart request may a browser form in which case each
 * part represents a text-based form field or a file upload. Multipart requests
 * may also be used outside of browsers to transfer data with any content type
 * such as JSON, PDF, etc.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578 (multipart/form-data)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183 (Content-Disposition)</a>
 * @see <a href="https://www.w3.org/TR/html5/forms.html#multipart-form-data">HTML5 (multipart forms)</a>
 */
public interface Part {

	/**
	 * Return the name of the part in the multipart form.
	 * @return the name of the part, never {@code null} or empty
	 */
	String getName();

	/**
	 * Return the headers associated with the part.
	 */
	HttpHeaders getHeaders();

	/**
	 *
	 * Return the name of the file selected by the user in a browser form.
	 * @return the filename if defined and available
	 */
	Optional<String> getFilename();

	/**
	 * Return the part content converted to a String with the charset from the
	 * {@code Content-Type} header or {@code UTF-8} by default.
	 */
	Mono<String> getContentAsString();

	/**
	 * Return the part raw content as a stream of DataBuffer's.
	 */
	Flux<DataBuffer> getContent();

	/**
	 * Transfer the file in this part to the given file destination.
	 * @param dest the target file
	 * @return completion {@code Mono} with the result of the file transfer,
	 * possibly {@link IllegalStateException} if the part isn't a file
	 */
	Mono<Void> transferTo(File dest);

}
