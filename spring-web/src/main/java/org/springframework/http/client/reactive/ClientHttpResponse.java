/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.client.reactive;

import java.io.Closeable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

/**
 * Represents a client-side reactive HTTP response.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpResponse extends ReactiveHttpInputMessage, Closeable {

	/**
	 * Return the HTTP status as an {@link HttpStatus} enum value.
	 */
	HttpStatus getStatusCode();

	/**
	 * Return a read-only map of response cookies received from the server.
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

	/**
	 * Close this response and the underlying HTTP connection.
	 * <p>This non-blocking method has to be called if its body isn't going
	 * to be consumed. Not doing so might result in HTTP connection pool
	 * inconsistencies or memory leaks.
	 * <p>This shouldn't be called if the response body is read,
	 * because it would prevent connections to be reused and cancel
	 * the benefits of using a connection pooling.
	 */
	@Override
	void close();

}
