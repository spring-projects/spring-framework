/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;

/**
 * Exception for errors that fit response status 413 (Content too large) for use in
 * Spring Web applications.
 *
 * @author Brian Clozel
 * @since 7.0
 */
@SuppressWarnings("serial")
public class ContentTooLargeException extends ResponseStatusException {

	public ContentTooLargeException(@Nullable Throwable cause) {
		super(HttpStatus.CONTENT_TOO_LARGE, null, cause);
	}

}
