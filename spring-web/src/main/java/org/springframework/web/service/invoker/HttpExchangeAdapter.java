/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

/**
 * Contract to abstract an HTTP client from {@linkplain HttpServiceProxyFactory}
 * and make it pluggable.
 *
 * <p>For reactive clients, see {@link ReactorHttpExchangeAdapter}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public interface HttpExchangeAdapter {

	/**
	 * Whether the underlying client supports use of request attributes.
	 */
	boolean supportsRequestAttributes();

	/**
	 * Perform the given request, and release the response content, if any.
	 * @param requestValues the request to perform
	 */
	void exchange(HttpRequestValues requestValues);

	/**
	 * Perform the given request, release the response content, and return the
	 * response headers.
	 * @param requestValues the request to perform
	 * @return the response headers
	 */
	HttpHeaders exchangeForHeaders(HttpRequestValues requestValues);

	/**
	 * Perform the given request and decode the response content to the given type.
	 * @param requestValues the request to perform
	 * @param bodyType the target type to decode to
	 * @param <T> the type the response is decoded to
	 * @return the decoded response body.
	 */
	@Nullable
	<T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

	/**
	 * Variant of {@link #exchange(HttpRequestValues)} with additional
	 * access to the response status and headers.
	 * @return the response entity with status and headers.
	 */
	ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues);

	/**
	 * Variant of {@link #exchangeForBody(HttpRequestValues, ParameterizedTypeReference)}
	 * with additional access to the response status and headers.
	 * @return the response entity with status, headers, and body.
	 */
	<T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

}
