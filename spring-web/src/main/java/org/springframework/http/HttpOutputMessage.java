/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents an HTTP output message, consisting of {@linkplain #getHeaders() headers}
 * and a writable {@linkplain #getBody() body}.
 *
 * <p>Typically implemented by an HTTP request handle on the client side,
 * or an HTTP response handle on the server side.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpOutputMessage extends HttpMessage {

	/**
	 * Return the body of the message as an output stream.
	 * @return the output stream body (never {@code null})
	 * @throws IOException in case of I/O errors
	 */
	OutputStream getBody() throws IOException;

}
