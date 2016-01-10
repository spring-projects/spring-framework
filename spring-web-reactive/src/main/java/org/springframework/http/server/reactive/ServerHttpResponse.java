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

package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;

/**
 * Represents a "reactive" server-side HTTP response.
 *
 * @author Arjen Poutsma
 */
public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

	/**
	 * Set the HTTP status code of the response.
	 * @param status the HTTP status as an {@link HttpStatus} enum value
	 */
	void setStatusCode(HttpStatus status);

	/**
	 * Use this method to apply header changes made via {@link #getHeaders()} to
	 * the underlying server response. By default changes made via
	 * {@link #getHeaders()} are cached until a call to {@link #setBody}
	 * implicitly applies header changes or until this method is called.
	 *
	 * <p><strong>Note:</strong> After this method is called,
	 * {@link #getHeaders() headers} become read-only and any additional calls
	 * to this method are ignored.
	 */
	void writeHeaders();

}
