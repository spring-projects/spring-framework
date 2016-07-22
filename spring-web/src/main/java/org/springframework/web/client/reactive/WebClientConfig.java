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

import java.util.List;

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

/**
 * Interface that makes the {@link WebClient} configuration information
 * available to downstream infrastructure such as {@link ResponseErrorHandler}s.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface WebClientConfig {

	/**
	 * Return the message readers that can help decoding the HTTP response body
	 */
	List<HttpMessageReader<?>> getMessageReaders();

	/**
	 * Return the message writers that can help encode the HTTP request body
	 */
	List<HttpMessageWriter<?>> getMessageWriters();

	/**
	 * Return the configured {@link ResponseErrorHandler}
	 */
	ResponseErrorHandler getResponseErrorHandler();
}
