/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.client.ClientHttpRequest;

/**
 * Callback interface for code that operates on a {@link ClientHttpRequest}.
 * Allows manipulating the request headers, and write to the request body.
 *
 * <p>Used internally by the {@link RestTemplate}, but also useful for
 * application code. There several available factory methods:
 * <ul>
 * <li>{@link RestTemplate#acceptHeaderRequestCallback(Class)}
 * <li>{@link RestTemplate#httpEntityCallback(Object)}
 * <li>{@link RestTemplate#httpEntityCallback(Object, Type)}
 * </ul>
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see RestTemplate#execute
 */
@FunctionalInterface
public interface RequestCallback {

	/**
	 * Gets called by {@link RestTemplate#execute} with an opened {@code ClientHttpRequest}.
	 * Does not need to care about closing the request or about handling errors:
	 * this will all be handled by the {@code RestTemplate}.
	 * @param request the active HTTP request
	 * @throws IOException in case of I/O errors
	 */
	void doWithRequest(ClientHttpRequest request) throws IOException;

}
