/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.Closeable;
import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatusCode;

/**
 * Represents a client-side HTTP response.
 *
 * <p>Obtained via an invocation of {@link ClientHttpRequest#execute()}.
 *
 * <p>A {@code ClientHttpResponse} must be {@linkplain #close() closed},
 * typically in a {@code finally} block.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ClientHttpResponse extends HttpInputMessage, Closeable {

	/**
	 * Get the HTTP status code as an {@link HttpStatusCode}.
	 * @return the HTTP status as {@code HttpStatusCode} value (never {@code null})
	 * @throws IOException in case of I/O errors
	 */
	HttpStatusCode getStatusCode() throws IOException;

	/**
	 * Get the HTTP status text of the response.
	 * @return the HTTP status text
	 * @throws IOException in case of I/O errors
	 */
	String getStatusText() throws IOException;

	/**
	 * Close this response, freeing any resources created.
	 */
	@Override
	void close();

}
