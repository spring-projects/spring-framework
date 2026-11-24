/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.multipart;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;

/**
 * Representation of a part in a multipart HTTP message.
 *
 * <p>The origin of a multipart request may be a browser form in which case each
 * part is either a {@link FormFieldPart} or a {@link FilePart}.
 *
 * <p>Multipart requests may also be used outside a browser for data of any
 * content type (for example, JSON, PDF, etc).
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
	 * Return the name of the part from its {@code Content-Disposition} header.
	 * <p>For parts that may have no name, use
	 * {@code headers().getContentDisposition().getName()}.
	 * @return the name of the part, never {@code null} or empty
	 * @throws IllegalStateException if the part has no name
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

	/**
	 * Return a mono that, when subscribed to, deletes the underlying storage
	 * for this part.
	 * @since 5.3.13
	 */
	default Mono<Void> delete() {
		return Mono.empty();
	}

}
