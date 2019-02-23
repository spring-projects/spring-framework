/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * A function that can extract data from a {@link ReactiveHttpInputMessage} body.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the type of data to extract
 * @param <M> the type of {@link ReactiveHttpInputMessage} this extractor can be applied to
 * @see BodyExtractors
 */
@FunctionalInterface
public interface BodyExtractor<T, M extends ReactiveHttpInputMessage> {

	/**
	 * Extract from the given input message.
	 * @param inputMessage the request to extract from
	 * @param context the configuration to use
	 * @return the extracted data
	 */
	T extract(M inputMessage, Context context);


	/**
	 * Defines the context used during the extraction.
	 */
	interface Context {

		/**
		 * Return the {@link HttpMessageReader HttpMessageReaders} to be used for body extraction.
		 * @return the stream of message readers
		 */
		List<HttpMessageReader<?>> messageReaders();

		/**
		 * Optionally return the {@link ServerHttpResponse}, if present.
		 */
		Optional<ServerHttpResponse> serverResponse();

		/**
		 * Return the map of hints to use to customize body extraction.
		 */
		Map<String, Object> hints();
	}

}
