/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import org.jspecify.annotations.Nullable;

import org.springframework.util.InvalidMimeTypeException;

/**
 * Exception thrown from {@link MediaType#parseMediaType(String)} in case of
 * encountering an invalid media type specification String.
 *
 * @author Juergen Hoeller
 * @since 3.2.2
 */
@SuppressWarnings("serial")
public class InvalidMediaTypeException extends IllegalArgumentException {

	private final String mediaType;


	/**
	 * Create a new InvalidMediaTypeException for the given media type.
	 * @param mediaType the offending media type
	 * @param message a detail message indicating the invalid part
	 */
	public InvalidMediaTypeException(String mediaType, @Nullable String message) {
		super(message != null ? "Invalid media type \"" + mediaType + "\": " + message : "Invalid media type \"" + mediaType);
		this.mediaType = mediaType;
	}

	/**
	 * Constructor that allows wrapping {@link InvalidMimeTypeException}.
	 */
	InvalidMediaTypeException(InvalidMimeTypeException ex) {
		super(ex.getMessage(), ex);
		this.mediaType = ex.getMimeType();
	}

	/**
	 * Returns the detail message string of this exception instance (never {@code null}).
	 */
	@Override
	@SuppressWarnings("NullAway") // InvalidMediaTypeException message is never null
	public String getMessage() {
		return super.getMessage();
	}

	/**
	 * Return the offending media type.
	 */
	public String getMediaType() {
		return this.mediaType;
	}

}
