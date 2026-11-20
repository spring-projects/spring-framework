/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

/**
 * Exception raised when an API version is valid, but did not match the versions
 * declared in request mappings for the endpoint. This can happen when a
 * controller method is mapped with a fixed version, e.g. "2", but the request
 * is for a higher version.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@SuppressWarnings("serial")
public class NotAcceptableApiVersionException extends InvalidApiVersionException {


	public NotAcceptableApiVersionException(String version) {
		super(version);
	}

}
