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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Spring's default implementation of the {@link ResponseErrorHandler} interface.
 *
 * <p>This error handler checks for the status code on the
 * {@link ClientHttpResponse}. Any code in the 4xx or 5xx series is considered
 * to be an error. This behavior can be changed by overriding
 * {@link #hasError(HttpStatusCode)}. Unknown status codes will be ignored by
 * {@link #hasError(ClientHttpResponse)}.
 *
 * <p>See {@link #handleError(URI, HttpMethod, ClientHttpResponse)}  for more
 * details on specific exception types.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 * @see RestTemplate#setErrorHandler
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

	private @Nullable List<HttpMessageConverter<?>> messageConverters;


	/**
	 * For internal use from the RestTemplate, to pass the message converters
	 * to use to decode error content.
	 * @since 6.0
	 */
	void setMessageConverters(List<HttpMessageConverter<?>> converters) {
		this.messageConverters = Collections.unmodifiableList(converters);
	}


	/**
	 * Delegates to {@link #hasError(HttpStatusCode)} with the response status code.
	 * @see ClientHttpResponse#getStatusCode()
	 * @see #hasError(HttpStatusCode)
	 */
	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		HttpStatusCode statusCode = response.getStatusCode();
		return hasError(statusCode);
	}

	/**
	 * Template method called from {@link #hasError(ClientHttpResponse)}.
	 * <p>The default implementation checks {@link HttpStatusCode#isError()}.
	 * Can be overridden in subclasses.
	 * @param statusCode the HTTP status code
	 * @return {@code true} if the response indicates an error; {@code false} otherwise
	 * @see HttpStatusCode#isError()
	 */
	protected boolean hasError(HttpStatusCode statusCode) {
		return statusCode.isError();
	}

	/**
	 * Handle the error in the given response with the given resolved status code
	 * and extra information providing access to the request URL and HTTP method.
	 * <p>The default implementation throws:
	 * <ul>
	 * <li>{@link HttpClientErrorException} if the status code is in the 4xx
	 * series, or one of its sub-classes such as
	 * {@link HttpClientErrorException.BadRequest} and others.
	 * <li>{@link HttpServerErrorException} if the status code is in the 5xx
	 * series, or one of its sub-classes such as
	 * {@link HttpServerErrorException.InternalServerError} and others.
	 * <li>{@link UnknownHttpStatusCodeException} for error status codes not in the
	 * {@link HttpStatus} enum range.
	 * </ul>
	 * @throws UnknownHttpStatusCodeException in case of an unresolvable status code
	 * @since 6.2
	 * @see #handleError(ClientHttpResponse, HttpStatusCode, URI, HttpMethod)
	 */
	@Override
	public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		handleError(response, response.getStatusCode(), url, method);
	}

	/**
	 * Handle the error based on the resolved status code.
	 * <p>The default implementation delegates to
	 * {@link HttpClientErrorException#create} for errors in the 4xx range, to
	 * {@link HttpServerErrorException#create} for errors in the 5xx range,
	 * or otherwise raises {@link UnknownHttpStatusCodeException}.
	 * @since 6.2
	 * @see HttpClientErrorException#create
	 * @see HttpServerErrorException#create
	 */
	protected void handleError(
			ClientHttpResponse response, HttpStatusCode statusCode,
			@Nullable URI url, @Nullable HttpMethod method) throws IOException {

		String statusText = response.getStatusText();
		HttpHeaders headers = response.getHeaders();
		byte[] body = getResponseBody(response);
		Charset charset = getCharset(response);
		String message = getErrorMessage(statusCode.value(), statusText, body, charset, url, method);

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

		if (!CollectionUtils.isEmpty(this.messageConverters)) {
			ex.setBodyConvertFunction(initBodyConvertFunction(response, body));
		}

		throw ex;
	}

	/**
	 * Read the body of the given response (for inclusion in a status exception).
	 * @param response the response to inspect
	 * @return the response body as a byte array,
	 * or an empty byte array if the body could not be read
	 * @since 4.3.8
	 */
	protected byte[] getResponseBody(ClientHttpResponse response) {
		return RestClientUtils.getBody(response);
	}

	/**
	 * Determine the charset of the response (for inclusion in a status exception).
	 * @param response the response to inspect
	 * @return the associated charset, or {@code null} if none
	 * @since 4.3.8
	 */
	protected @Nullable Charset getCharset(ClientHttpResponse response) {
		MediaType contentType = response.getHeaders().getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}

	/**
	 * Return an error message with details from the response body. For example:
	 * <pre>
	 * 404 Not Found on GET request for "https://example.com": [{'id': 123, 'message': 'my message'}]
	 * </pre>
	 */
	private String getErrorMessage(
			int rawStatusCode, String statusText, byte @Nullable [] responseBody, @Nullable Charset charset,
			@Nullable URI url, @Nullable HttpMethod method) {

		StringBuilder msg = new StringBuilder(rawStatusCode + " " + statusText);
		if (method != null) {
			msg.append(" on ").append(method).append(" request");
		}
		if (url != null) {
			msg.append(" for \"");
			String urlString = url.toString();
			int idx = urlString.indexOf('?');
			if (idx != -1) {
				msg.append(urlString, 0, idx);
			}
			else {
				msg.append(urlString);
			}
			msg.append("\"");
		}
		msg.append(": ");
		if (ObjectUtils.isEmpty(responseBody)) {
			msg.append("[no body]");
		}
		else {
			charset = (charset != null ? charset : StandardCharsets.UTF_8);
			String bodyText = new String(responseBody, charset);
			bodyText = LogFormatUtils.formatValue(bodyText, -1, true);
			msg.append(bodyText);
		}
		return msg.toString();
	}

	/**
	 * Return a function for decoding the error content. This can be passed to
	 * {@link RestClientResponseException#setBodyConvertFunction(Function)}.
	 * @since 6.0
	 */
	@SuppressWarnings("NullAway") // Lambda
	protected Function<ResolvableType, ?> initBodyConvertFunction(ClientHttpResponse response, byte[] body) {
		Assert.state(!CollectionUtils.isEmpty(this.messageConverters), "Expected message converters");
		return resolvableType -> {
			try {
				HttpMessageConverterExtractor<?> extractor =
						new HttpMessageConverterExtractor<>(resolvableType.getType(), this.messageConverters);

				return extractor.extractData(new ClientHttpResponseDecorator(response) {
					@Override
					public InputStream getBody() {
						return new ByteArrayInputStream(body);
					}
				});
			}
			catch (IOException ex) {
				throw new RestClientException(
						"Error while extracting response for type [" + resolvableType + "]", ex);
			}
		};
	}

}
