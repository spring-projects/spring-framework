/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

/**
 * Represents the base interface for HTTP request and response messages.
 * Consists of {@link HttpHeaders}, retrievable via {@link #getHeaders()}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpMessage {

	/**
	 * Return the headers of this message.
	 * @return a corresponding HttpHeaders object (never {@code null})
	 */
	HttpHeaders getHeaders();

}
