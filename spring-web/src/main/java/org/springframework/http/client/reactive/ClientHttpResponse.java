/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.reactive;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Represents a client-side reactive HTTP response.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpResponse extends ReactiveHttpInputMessage {

	/**
	 * Return an id that represents the underlying connection, if available,
	 * or the request for the purpose of correlating log messages.
	 * @since 5.3.5
	 */
	default String getId() {
		return ObjectUtils.getIdentityHexString(this);
	}

	/**
	 * Return the HTTP status code as an {@link HttpStatusCode}.
	 * @return the HTTP status as {@code HttpStatusCode} value (never {@code null})
	 */
	HttpStatusCode getStatusCode();

	/**
	 * Return a read-only map of response cookies received from the server.
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

}
