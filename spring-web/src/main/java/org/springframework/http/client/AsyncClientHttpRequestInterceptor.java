/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.InterceptingAsyncHttpAccessor;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Intercepts client-side HTTP requests. Implementations of this interface can be
 * {@linkplain org.springframework.web.client.AsyncRestTemplate#setInterceptors registered}
 * with the {@link org.springframework.web.client.AsyncRestTemplate} as to modify
 * the outgoing {@link HttpRequest} and/or register to modify the incoming
 * {@link ClientHttpResponse} with help of a
 * {@link org.springframework.util.concurrent.ListenableFutureAdapter}.
 *
 * <p>The main entry point for interceptors is {@link #intercept}.
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see org.springframework.web.client.AsyncRestTemplate
 * @see InterceptingAsyncHttpAccessor
 */
public interface AsyncClientHttpRequestInterceptor {

	/**
	 * Intercept the given request, and return a response future. The given
	 * {@link AsyncClientHttpRequestExecution} allows the interceptor to pass on
	 * the request to the next entity in the chain.
	 * <p>An implementation might follow this pattern:
	 * <ol>
	 * <li>Examine the {@linkplain HttpRequest request} and body</li>
	 * <li>Optionally {@linkplain org.springframework.http.client.support.HttpRequestWrapper
	 * wrap} the request to filter HTTP attributes.</li>
	 * <li>Optionally modify the body of the request.</li>
	 * <li>One of the following:
	 * <ul>
	 * <li>execute the request through {@link ClientHttpRequestExecution}</li>
	 * <li>don't execute the request to block the execution altogether</li>
	 * </ul>
	 * <li>Optionally adapt the response to filter HTTP attributes with the help of
	 * {@link org.springframework.util.concurrent.ListenableFutureAdapter
	 * ListenableFutureAdapter}.</li>
	 * </ol>
	 * @param request the request, containing method, URI, and headers
	 * @param body the body of the request
	 * @param execution the request execution
	 * @return the response future
	 * @throws IOException in case of I/O errors
	 */
	ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body,
			AsyncClientHttpRequestExecution execution) throws IOException;

}
