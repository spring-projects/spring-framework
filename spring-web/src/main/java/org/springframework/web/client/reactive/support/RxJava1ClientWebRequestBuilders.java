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

package org.springframework.web.client.reactive.support;

import org.springframework.http.HttpMethod;

/**
 * Static factory methods for {@link RxJava1ClientWebRequestBuilder ClientWebRequestBuilders}
 * using the {@link rx.Observable} and {@link rx.Single} API.
 *
 * @author Brian Clozel
 */
public abstract class RxJava1ClientWebRequestBuilders {

	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for a GET request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder get(String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(HttpMethod.GET, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for a POST request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder post(String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(HttpMethod.POST, urlTemplate, urlVariables);
	}


	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for a PUT request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder put(String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(HttpMethod.PUT, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for a PATCH request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder patch(String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(HttpMethod.PATCH, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for a DELETE request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder delete(String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(HttpMethod.DELETE, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for an OPTIONS request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder options(String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(HttpMethod.OPTIONS, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for a HEAD request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder head(String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(HttpMethod.HEAD, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link RxJava1ClientWebRequestBuilder} for a request with the given HTTP method.
	 *
	 * @param httpMethod   the HTTP method
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static RxJava1ClientWebRequestBuilder request(HttpMethod httpMethod, String urlTemplate, Object... urlVariables) {
		return new RxJava1ClientWebRequestBuilder(httpMethod, urlTemplate, urlVariables);
	}
}
