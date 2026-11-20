/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception raised when an API version is required, but is not present.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@SuppressWarnings("serial")
public class MissingApiVersionException extends ResponseStatusException {

	public MissingApiVersionException() {
		super(HttpStatus.BAD_REQUEST, "API version is required.");
	}

}
