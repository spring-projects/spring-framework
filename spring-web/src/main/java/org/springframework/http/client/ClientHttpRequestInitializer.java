/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.http.client.support.HttpAccessor;

/**
 * Callback interface for initializing a {@link ClientHttpRequest} prior to it
 * being used.
 *
 * <p>Typically used with {@link HttpAccessor} and subclasses such as
 * {@link org.springframework.web.client.RestTemplate RestTemplate} to apply
 * consistent settings or headers to each request.
 *
 * <p>Unlike {@link ClientHttpRequestInterceptor}, this interface can apply
 * customizations without needing to read the entire request body into memory.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see HttpAccessor#getClientHttpRequestInitializers()
 */
@FunctionalInterface
public interface ClientHttpRequestInitializer {

	/**
	 * Initialize the given client HTTP request.
	 * @param request the request to configure
	 */
	void initialize(ClientHttpRequest request);

}
