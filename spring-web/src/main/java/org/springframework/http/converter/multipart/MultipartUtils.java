/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.multipart;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Various static utility methods for dealing with multipart parsing.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 7.1
 */
abstract class MultipartUtils {

	/**
	 * Return the character set of the given headers, as defined in the
	 * {@link HttpHeaders#getContentType()} header.
	 */
	static Charset charset(HttpHeaders headers) {
		MediaType contentType = headers.getContentType();
		if (contentType != null) {
			Charset charset = contentType.getCharset();
			if (charset != null) {
				return charset;
			}
		}
		return StandardCharsets.UTF_8;
	}

}
