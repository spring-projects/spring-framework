/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.client;

import java.io.Closeable;
import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpInputMessage;

/**
 * Represents a "reactive" client-side HTTP response.
 *
 * @author Arjen Poutsma
 */
public interface ReactiveClientHttpResponse extends ReactiveHttpInputMessage, Closeable {

	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an HttpStatus enum value
	 * @throws IOException in case of I/O errors
	 */
	HttpStatus getStatusCode() throws IOException;

	/**
	 * Return the HTTP status code of the response as integer
	 * @return the HTTP status as an integer
	 * @throws IOException in case of I/O errors
	 */
	int getRawStatusCode() throws IOException;

	/**
	 * Return the HTTP status text of the response.
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
