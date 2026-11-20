/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.multipart;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;


/**
 * Representation for a part in a "multipart/form-data" request.
 *
 * <p>The origin of a multipart request may be a browser form in which case each
 * part is either a {@link FormFieldPart} or a {@link FilePart}.
 *
 * <p>Multipart requests may also be used outside a browser for data of any
 * content type (for example, JSON, PDF, etc).
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 7.1
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
	InputStream content() throws IOException;

	/**
	 * Delete the underlying storage for this part.
	 */
	default void delete() throws IOException {
	}

}
