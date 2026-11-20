/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception raised when an API version cannot be parsed, or is not in the
 * supported version set.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@SuppressWarnings("serial")
public class InvalidApiVersionException extends ResponseStatusException {

	private final String version;


	public InvalidApiVersionException(String version) {
		this(version, null, null);
	}

	public InvalidApiVersionException(String version, @Nullable String msg, @Nullable Exception cause) {
		super(HttpStatus.BAD_REQUEST, (msg != null ? msg : "Invalid API version: '" + version + "'."), cause);
		this.version = version;
	}


	/**
	 * Return the requested version.
	 */
	public String getVersion() {
		return this.version;
	}

}
