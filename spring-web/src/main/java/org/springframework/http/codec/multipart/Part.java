/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.codec.multipart;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;

/**
 * Representation for a part in a "multipart/form-data" request.
 *
 * <p>The origin of a multipart request may be a browser form in which case each
 * part is either a {@link FormFieldPart} or a {@link FilePart}.
 *
 * <p>Multipart requests may also be used outside of a browser for data of any
 * content type (e.g. JSON, PDF, etc).
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
	String name();

	/**
	 * Return the headers associated with the part.
	 */
	HttpHeaders headers();

	/**
	 * Return the content for this part.
	 * <p>Note that for a {@link FormFieldPart} the content may be accessed
	 * more easily via {@link FormFieldPart#value()}.
	 */
	Flux<DataBuffer> content();

}
