/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Used by {@link DefaultRestClient} and {@link DefaultRestClientBuilder}.
 *
 * @author Arjen Poutsma
 * @since 6.1
 */
final class StatusHandler {

	private final ResponsePredicate predicate;

	private final RestClient.ResponseSpec.ErrorHandler errorHandler;


	private StatusHandler(ResponsePredicate predicate, RestClient.ResponseSpec.ErrorHandler errorHandler) {
		this.predicate = predicate;
		this.errorHandler = errorHandler;
	}


	public static StatusHandler of(Predicate<HttpStatusCode> predicate,
			RestClient.ResponseSpec.ErrorHandler errorHandler) {
		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(errorHandler, "ErrorHandler must not be null");

		return new StatusHandler(response -> predicate.test(response.getStatusCode()), errorHandler);
	}

	public static StatusHandler fromErrorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ResponseErrorHandler must not be null");

		return new StatusHandler(errorHandler::hasError, (request, response) ->
				errorHandler.handleError(request.getURI(), request.getMethod(), response));
	}

	public static StatusHandler defaultHandler(List<HttpMessageConverter<?>> messageConverters) {
		return new StatusHandler(response -> response.getStatusCode().isError(),
				(request, response) -> {
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
					if (!CollectionUtils.isEmpty(messageConverters)) {
						ex.setBodyConvertFunction(initBodyConvertFunction(response, body, messageConverters));
					}
					throw ex;
				});
	}

	private static Function<ResolvableType, ?> initBodyConvertFunction(ClientHttpResponse response, byte[] body, List<HttpMessageConverter<?>> messageConverters) {
		Assert.state(!CollectionUtils.isEmpty(messageConverters), "Expected message converters");
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


	private static String getErrorMessage(int rawStatusCode, String statusText, @Nullable byte[] responseBody,
			@Nullable Charset charset) {

		String preface = rawStatusCode + " " + statusText + ": ";

		if (ObjectUtils.isEmpty(responseBody)) {
			return preface + "[no body]";
		}

		charset = (charset != null ? charset : StandardCharsets.UTF_8);

		String bodyText = new String(responseBody, charset);
		bodyText = LogFormatUtils.formatValue(bodyText, -1, true);

		return preface + bodyText;
	}



	public boolean test(ClientHttpResponse response) throws IOException {
		return this.predicate.test(response);
	}

	public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
		this.errorHandler.handle(request, response);
	}


	@FunctionalInterface
	private interface ResponsePredicate {

		boolean test(ClientHttpResponse response) throws IOException;
	}

}
