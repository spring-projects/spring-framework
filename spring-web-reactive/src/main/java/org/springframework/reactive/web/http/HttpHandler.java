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

import org.reactivestreams.Publisher;

import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.http.server.ReactiveServerHttpResponse;

/**
 * Interface for handlers that process HTTP requests and generate an HTTP response.
 * This handler is designed to be called when the HTTP headers have been received, making
 * the HTTP request body available as stream. The HTTP response body can also be written
 * as a stream.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @see ReactiveServerHttpRequest#getBody()
 * @see ReactiveServerHttpResponse#setBody(Publisher)
 */
public interface HttpHandler {

	/**
	 * Process the given request, generating a response in an asynchronous non blocking way.
	 * Implementations should not throw exceptions but signal them via the returned
	 * {@code Publisher<Void>}.
	 *
	 * @param request current HTTP request, the body can be processed as a data stream.
	 * @param response current HTTP response, the body can be provided as a data stream.
	 * @return A {@code Publisher<Void>} used to signal the demand, and receive a notification
	 * when the handling is complete (success or error) including the flush of the data on the
	 * network.
	 */
	Publisher<Void> handle(ReactiveServerHttpRequest request, ReactiveServerHttpResponse response);

}
