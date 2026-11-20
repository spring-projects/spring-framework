/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server;


import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;

/**
 * Exception for errors that fit response status 413 (payload too large) for use in
 * Spring Web applications.
 *
 * @author Kim Bosung
 * @since 6.2
 * @deprecated since 7.0 in favor of {@link ContentTooLargeException}
 */
@SuppressWarnings("serial")
@Deprecated(since = "7.0")
public class PayloadTooLargeException extends ResponseStatusException {

	public PayloadTooLargeException(@Nullable Throwable cause) {
		super(HttpStatus.PAYLOAD_TOO_LARGE, null, cause);
	}

}
