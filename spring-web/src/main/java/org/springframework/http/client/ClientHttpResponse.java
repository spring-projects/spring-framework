/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
