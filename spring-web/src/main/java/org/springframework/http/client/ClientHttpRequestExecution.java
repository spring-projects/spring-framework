/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.IOException;

import org.springframework.http.HttpRequest;

/**
 * Represents the context of a client-side HTTP request execution.
 *
 * <p>Used to invoke the next interceptor in the interceptor chain, or - if the calling interceptor is last - execute
 * the request itself.
 *
 * @author Arjen Poutsma
 * @see ClientHttpRequestInterceptor
 * @since 3.1
 */
@FunctionalInterface
public interface ClientHttpRequestExecution {

	/**
	 * Execute the request with the given request attributes and body, and return the response.
	 *
	 * @param request the request, containing method, URI, and headers
	 * @param body the body of the request to execute
	 * @return the response
	 * @throws IOException in case of I/O errors
	 */
	ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException;
}
