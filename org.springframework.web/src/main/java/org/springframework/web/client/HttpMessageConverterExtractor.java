/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;

/**
 * Response extractor that uses the given {@linkplain HttpMessageConverter entity converters} to convert the response
 * into a type <code>T</code>.
 *
 * @author Arjen Poutsma
 * @see RestTemplate
 * @since 3.0
 */
public class HttpMessageConverterExtractor<T> implements ResponseExtractor<T> {

	private final Class<T> responseType;

	private final List<HttpMessageConverter<?>> messageConverters;

	/**
	 * Creates a new instance of the {@code HttpMessageConverterExtractor} with the given response type and message
	 * converters. The given converters must support the response type.
	 */
	public HttpMessageConverterExtractor(Class<T> responseType, List<HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(responseType, "'responseType' must not be null");
		Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
		this.responseType = responseType;
		this.messageConverters = messageConverters;
	}

	@SuppressWarnings("unchecked")
	public T extractData(ClientHttpResponse response) throws IOException {
		MediaType contentType = response.getHeaders().getContentType();
		if (contentType == null) {
			throw new RestClientException("Cannot extract response: no Content-Type found");
		}
		for (HttpMessageConverter messageConverter : messageConverters) {
			if (messageConverter.canRead(responseType, contentType)) {
				return (T) messageConverter.read(this.responseType, response);
			}
		}
		throw new RestClientException(
				"Could not extract response: no suitable HttpMessageConverter found for response type [" +
						this.responseType.getName() + "] and content type [" + contentType + "]");
	}

}
