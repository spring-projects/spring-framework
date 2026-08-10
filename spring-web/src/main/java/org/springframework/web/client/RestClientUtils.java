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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.util.FileCopyUtils;

/**
 * Internal methods shared between types in this package.
 *
 * @author Arjen Poutsma
 * @since 6.1
 */
abstract class RestClientUtils {

	private static final Log logger = LogFactory.getLog(DefaultRestClient.class);


	public static byte[] getBody(HttpInputMessage message) {
		try {
			return FileCopyUtils.copyToByteArray(message.getBody());
		}
		catch (IOException ignore) {
		}
		return new byte[0];
	}

	public static @Nullable Charset getCharset(HttpMessage response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}

	/**
	 * Read the body of the given response into the given target type, using
	 * the given message converters.
	 * @return the converted body, or {@code null} if the response has no message body
	 * @throws UnknownContentTypeException if no converter can read the target type
	 * @throws RestClientException if the conversion fails
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	static <T> @Nullable T readWithMessageConverters(ClientHttpResponse response,
			List<HttpMessageConverter<?>> messageConverters, Type bodyType, Class<T> bodyClass,
			@Nullable Map<String, Object> hints) throws IOException {

		IntrospectingClientHttpResponse responseWrapper = new IntrospectingClientHttpResponse(response);
		if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
			return null;
		}
		MediaType contentType = getContentType(responseWrapper);

		try {
			for (HttpMessageConverter<?> messageConverter : messageConverters) {
				if (messageConverter instanceof GenericHttpMessageConverter genericMessageConverter) {
					if (genericMessageConverter.canRead(bodyType, null, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Reading to [" + ResolvableType.forType(bodyType) + "]");
						}
						return (T) genericMessageConverter.read(bodyType, null, responseWrapper);
					}
				}
				else if (messageConverter instanceof SmartHttpMessageConverter smartMessageConverter) {
					ResolvableType resolvableType = ResolvableType.forType(bodyType);
					if (smartMessageConverter.canRead(resolvableType, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Reading to [" + resolvableType + "]");
						}
						return (T) smartMessageConverter.read(resolvableType, responseWrapper, hints);
					}
				}
				else if (messageConverter.canRead(bodyClass, contentType)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Reading to [" + bodyClass.getName() + "] as \"" + contentType + "\"");
					}
					return (T) messageConverter.read((Class) bodyClass, responseWrapper);
				}
			}
		}
		catch (IOException | HttpMessageNotReadableException ex) {
			throw new RestClientException("Error while extracting response for type [" +
					ResolvableType.forType(bodyType) + "] and content type [" + contentType + "]", ex);
		}

		throw new UnknownContentTypeException(bodyType, contentType,
				responseWrapper.getStatusCode(), responseWrapper.getStatusText(),
				responseWrapper.getHeaders(), getBody(responseWrapper));
	}

	static MediaType getContentType(HttpMessage message) {
		MediaType contentType = message.getHeaders().getContentType();
		return (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
	}

}
