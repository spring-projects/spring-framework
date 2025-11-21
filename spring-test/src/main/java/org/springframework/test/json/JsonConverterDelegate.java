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

package org.springframework.test.json;

import java.io.IOException;

import org.springframework.core.ResolvableType;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Delegate to abstract JSON type conversion in AssertJ support clases.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface JsonConverterDelegate {

	/**
	 * Convert JSON content to the given {@code targetType}.
	 * @param content the JSON content
	 * @param targetType the target type
	 * @return the decoded object
	 * @param <T> the target type
	 */
	<T> T read(String content, ResolvableType targetType) throws IOException;

	/**
	 * Map the given Object value to the given {@code targetType}, via
	 * serialization and deserialization to and from JSON. This is useful for
	 * mapping generic maps and lists to higher level Objects.
	 * @param value the value to map
	 * @param targetType the target tyep
	 * @return the decoded object
	 * @param <T> the target type
	 */
	<T> T map(Object value, ResolvableType targetType) throws IOException;


	/**
	 * Create a {@link JsonConverterDelegate} from message converters.
	 * @param candidates the candidates
	 */
	static JsonConverterDelegate of(Iterable<HttpMessageConverter<?>> candidates) {
		return new DefaultJsonConverterDelegate(candidates);
	}

}
