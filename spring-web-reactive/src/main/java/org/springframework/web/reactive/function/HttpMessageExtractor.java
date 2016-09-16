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

package org.springframework.web.reactive.function;

import org.springframework.http.ReactiveHttpInputMessage;

/**
 * Contract to extract the content of a raw {@link ReactiveHttpInputMessage} decoding
 * the request body and using a target composition API.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface HttpMessageExtractor<T, R extends ReactiveHttpInputMessage> {

	/**
	 * Extract content from the response body
	 * @param message the raw HTTP message
	 * @return the extracted content
	 */
	T extract(R message);

}
