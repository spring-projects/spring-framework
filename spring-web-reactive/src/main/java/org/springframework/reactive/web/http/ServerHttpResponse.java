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
package org.springframework.reactive.web.http;

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;

import org.springframework.http.HttpStatus;

/**
 * Represent a server-side HTTP response.
 *
 * @author Rossen Stoyanchev
 */
public interface ServerHttpResponse extends HttpMessage {

	void setStatusCode(HttpStatus status);

	/**
	 * Write the response headers. This method must be invoked to send responses without body.
	 * @return A {@code Publisher<Void>} used to signal the demand, and receive a notification
	 * when the handling is complete (success or error) including the flush of the data on the
	 * network.
	 */
	Publisher<Void> writeHeaders();

	/**
	 * Write the provided reactive stream of bytes to the response body. Most servers
	 * support multiple {@code writeWith} calls. Headers are written automatically
	 * before the body, so not need to call {@link #writeHeaders()} explicitly.
	 * @param contentPublisher the stream to write in the response body.
	 * @return A {@code Publisher<Void>} used to signal the demand, and receive a notification
	 * when the handling is complete (success or error) including the flush of the data on the
	 * network.
	 */
	Publisher<Void> writeWith(Publisher<ByteBuffer> contentPublisher);

}
