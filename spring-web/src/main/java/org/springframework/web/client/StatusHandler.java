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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Simple container for an error response Predicate and an error response handler
 * to support the status handling mechanism of {@link RestClient.ResponseSpec}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 6.1
 */
final class StatusHandler {

	private final ResponsePredicate predicate;

	private final RestClient.ResponseSpec.ErrorHandler handler;


	private StatusHandler(ResponsePredicate predicate, RestClient.ResponseSpec.ErrorHandler handler) {
		this.predicate = predicate;
		this.handler = handler;
	}


	/**
	 * Test whether the response has any errors.
	 */
	public boolean test(ClientHttpResponse response) throws IOException {
		return this.predicate.test(response);
	}

	/**
	 * Handle the error in the given response.
	 * <p>This method is only called when {@link #test(ClientHttpResponse)}
	 * has returned {@code true}.
ß	 */
	public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
		this.handler.handle(request, response);
	}


	/**
	 * Create a StatusHandler from a RestClient {@link RestClient.ResponseSpec.ErrorHandler}.
	 */
	public static StatusHandler of(
			Predicate<HttpStatusCode> predicate, RestClient.ResponseSpec.ErrorHandler errorHandler) {

		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(errorHandler, "ErrorHandler must not be null");

		return new StatusHandler(response -> predicate.test(response.getStatusCode()), errorHandler);
	}

	/**
	 * Create a StatusHandler from a {@link ResponseErrorHandler}.
	 */
	public static StatusHandler fromErrorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ResponseErrorHandler must not be null");

		return new StatusHandler(errorHandler::hasError,
				(request, response) -> errorHandler.handleError(request.getURI(), request.getMethod(), response));
	}

	/**
	 * Create a StatusHandler for default error response handling.
	 */
	public static StatusHandler createDefaultStatusHandler(List<HttpMessageConverter<?>> converters) {
		return new StatusHandler(response -> response.getStatusCode().isError(),
				(request, response) -> {
					throw createException(response, converters);
				});
	}

	/**
	 * Create a {@link RestClientResponseException} of the appropriate
	 * subtype depending on the response status code.
	 * @param response the response
	 * @param converters the converters to use to decode the body
	 * @return the created exception
	 * @throws IOException in case of a response failure (e.g. to obtain the status)
	 * @since 7.0
	 */
	public static RestClientResponseException createException(
			ClientHttpResponse response, List<HttpMessageConverter<?>> converters) throws IOException {

		HttpStatusCode statusCode = response.getStatusCode();
		String statusText = response.getStatusText();
		HttpHeaders headers = response.getHeaders();
		byte[] body = RestClientUtils.getBody(response);
		Charset charset = RestClientUtils.getCharset(response);
		String message = getErrorMessage(statusCode.value(), statusText, body, charset);
		RestClientResponseException ex;

		if (statusCode.is4xxClientError()) {
			ex = HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
		}
		else if (statusCode.is5xxServerError()) {
			ex = HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
		}
		else {
			ex = new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
		}
		if (!CollectionUtils.isEmpty(converters)) {
			ex.setBodyConvertFunction(initBodyConvertFunction(response, body, converters));
		}
		return ex;
	}

	private static String getErrorMessage(
			int rawStatusCode, String statusText, byte @Nullable [] responseBody, @Nullable Charset charset) {

		String preface = rawStatusCode + " " + statusText + ": ";

		if (ObjectUtils.isEmpty(responseBody)) {
			return preface + "[no body]";
		}

		charset = (charset != null ? charset : StandardCharsets.UTF_8);

		String bodyText = new String(responseBody, charset);
		bodyText = LogFormatUtils.formatValue(bodyText, -1, true);

		return preface + bodyText;
	}

	@SuppressWarnings("NullAway")
	private static Function<ResolvableType, ? extends @Nullable Object> initBodyConvertFunction(
			ClientHttpResponse response, byte[] body, List<HttpMessageConverter<?>> messageConverters) {

		return resolvableType -> {
			try {
				HttpMessageConverterExtractor<?> extractor =
						new HttpMessageConverterExtractor<>(resolvableType.getType(), messageConverters);

				return extractor.extractData(new ClientHttpResponseDecorator(response) {
					@Override
					public InputStream getBody() {
						return new ByteArrayInputStream(body);
					}
				});
			}
			catch (IOException ex) {
				throw new RestClientException("Error while extracting response for type [" + resolvableType + "]", ex);
			}
		};
	}


	@FunctionalInterface
	private interface ResponsePredicate {

		boolean test(ClientHttpResponse response) throws IOException;
	}

}
