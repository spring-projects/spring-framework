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

package org.springframework.web.client.reactive;


import org.springframework.http.HttpMethod;

/**
 * Static factory methods for {@link DefaultClientWebRequestBuilder ClientWebRequestBuilders}
 *
 * @author Brian Clozel
 */
public abstract class ClientWebRequestBuilders {

	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for a GET request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder get(String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(HttpMethod.GET, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for a POST request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder post(String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(HttpMethod.POST, urlTemplate, urlVariables);
	}


	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for a PUT request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder put(String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(HttpMethod.PUT, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for a PATCH request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder patch(String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(HttpMethod.PATCH, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for a DELETE request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder delete(String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(HttpMethod.DELETE, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for an OPTIONS request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder options(String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(HttpMethod.OPTIONS, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for a HEAD request.
	 *
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder head(String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(HttpMethod.HEAD, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link DefaultClientWebRequestBuilder} for a request with the given HTTP method.
	 *
	 * @param httpMethod   the HTTP method
	 * @param urlTemplate  a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static DefaultClientWebRequestBuilder request(HttpMethod httpMethod, String urlTemplate, Object... urlVariables) {
		return new DefaultClientWebRequestBuilder(httpMethod, urlTemplate, urlVariables);
	}

}