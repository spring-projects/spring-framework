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

package org.springframework.http.converter.support;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;

/**
 * Extension of {@link MultipartHttpMessageConverter},
 * adding support for XML, JSON, Smile, CBOR, Protobuf and Yaml based parts when
 * related libraries are present in the classpath.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.2
 * @deprecated since 7.1 in favor of {@link MultipartHttpMessageConverter}.
 */
@Deprecated(since = "7.1", forRemoval = true)
public class AllEncompassingFormHttpMessageConverter extends MultipartHttpMessageConverter {


	/**
	 * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
	 * that will auto-detect part converters.
	 */
	public AllEncompassingFormHttpMessageConverter() {
		super(HttpMessageConverters.forClient().registerDefaults().build());
	}

	/**
	 * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
	 * using the given message converters.
	 * @param converters the message converters to use for part conversion
	 * @since 7.0
	 */
	public AllEncompassingFormHttpMessageConverter(Iterable<HttpMessageConverter<?>> converters) {
		super(converters);
	}

}
