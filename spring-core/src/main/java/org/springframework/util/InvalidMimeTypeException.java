/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown from {@link MimeTypeUtils#parseMimeType(String)} in case of
 * encountering an invalid content type specification String.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
@SuppressWarnings("serial")
public class InvalidMimeTypeException extends IllegalArgumentException {

	private final String mimeType;


	/**
	 * Create a new InvalidContentTypeException for the given content type.
	 * @param mimeType the offending media type
	 * @param message a detail message indicating the invalid part
	 */
	public InvalidMimeTypeException(String mimeType, @Nullable String message) {
		super(message == null ?
				"Invalid mime type \"" + mimeType + "\"" :
				"Invalid mime type \"" + mimeType + "\": " + message);
		this.mimeType = mimeType;
	}


	/**
	 * Return the offending content type.
	 */
	public String getMimeType() {
		return this.mimeType;
	}

}
