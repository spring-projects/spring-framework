/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.http.server;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatusCode;

/**
 * Represents a server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

	/**
	 * Set the HTTP status code of the response.
	 * @param status the HTTP status as an HttpStatus enum value
	 */
	void setStatusCode(HttpStatusCode status);

	/**
	 * Ensure that the headers and the content of the response are written out.
	 * <p>After the first flush, headers can no longer be changed.
	 * Only further content writing and content flushing is possible.
	 */
	@Override
	void flush() throws IOException;

	/**
	 * Close this response, freeing any resources created.
	 */
	@Override
	void close();

}
