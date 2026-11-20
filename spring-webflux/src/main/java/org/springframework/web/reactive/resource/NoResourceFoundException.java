/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Raised when {@link ResourceWebHandler} is mapped to the request but can not
 * find a matching resource.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
@SuppressWarnings("serial")
public class NoResourceFoundException extends ResponseStatusException {

	public NoResourceFoundException(URI uri, String resourcePath) {
		super(HttpStatus.NOT_FOUND,
				"No static resource " + resourcePath + " for request '" + uri + "'.",
				null, null, new Object[] { resourcePath});

		setDetail("No static resource " + resourcePath + ".");
	}

}
