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

import java.util.function.Consumer;

import org.springframework.http.HttpStatus;

/**
 * Allows applying actions, such as extractors, on the result of an executed
 * {@link WebClient} request.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface WebResponseActions {

	/**
	 * Apply synchronous operations once the HTTP response status
	 * has been received.
	 */
	void doWithStatus(Consumer<HttpStatus> consumer);

	/**
	 * Perform an extraction of the response body into a higher level representation.
	 *
	 * <pre class="code">
	 * static imports: ClientWebRequestBuilder.*, ResponseExtractors.*
	 *
	 * webClient
	 *   .perform(get(url).accept(MediaType.TEXT_PLAIN))
	 *   .extract(body(String.class));
	 * </pre>
	 */
	<T> T extract(ResponseExtractor<T> extractor);

}
