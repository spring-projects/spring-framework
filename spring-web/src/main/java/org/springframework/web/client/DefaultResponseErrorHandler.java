/*
 * Copyright 2002-2017 the original author or authors.
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
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;

/**
 * Spring's default implementation of the {@link ResponseErrorHandler} interface.
 *
 * <p>This error handler checks for the status code on the {@link ClientHttpResponse}:
 * Any code with series {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR} or
 * {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR} is considered to be an
 * error. This behavior can be changed by overriding the {@link #hasError(HttpStatus)} method.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see RestTemplate#setErrorHandler
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

	/**
	 * Delegates to {@link #hasError(HttpStatus)} with the response status code.
	 */
	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return hasError(getHttpStatusCode(response));
	}

	/**
	 * This default implementation throws a {@link HttpClientErrorException} if the response status code
	 * is {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR}, a {@link HttpServerErrorException}
	 * if it is {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR},
	 * and a {@link RestClientException} in other cases.
	 */
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		HttpStatus statusCode = getHttpStatusCode(response);
		switch (statusCode.series()) {
			case CLIENT_ERROR:
				throw new HttpClientErrorException(statusCode, response.getStatusText(),
						response.getHeaders(), getResponseBody(response), getCharset(response));
			case SERVER_ERROR:
				throw new HttpServerErrorException(statusCode, response.getStatusText(),
						response.getHeaders(), getResponseBody(response), getCharset(response));
			default:
				throw new RestClientException("Unknown status code [" + statusCode + "]");
		}
	}


	/**
	 * Determine the HTTP status of the given response.
	 * @param response the response to inspect
	 * @return the associated HTTP status
	 * @throws IOException in case of I/O errors
	 * @throws UnknownHttpStatusCodeException in case of an unknown status code
	 * that cannot be represented with the {@link HttpStatus} enum
	 * @since 4.3.8
	 */
	protected HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
		try {
			return response.getStatusCode();
		}
		catch (IllegalArgumentException ex) {
			throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
	}

	/**
	 * Template method called from {@link #hasError(ClientHttpResponse)}.
	 * <p>The default implementation checks if the given status code is
	 * {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR CLIENT_ERROR}
	 * or {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR SERVER_ERROR}.
	 * Can be overridden in subclasses.
	 * @param statusCode the HTTP status code
	 * @return {@code true} if the response has an error; {@code false} otherwise
	 * @see #getHttpStatusCode(ClientHttpResponse)
	 */
	protected boolean hasError(HttpStatus statusCode) {
		return (statusCode.series() == HttpStatus.Series.CLIENT_ERROR ||
				statusCode.series() == HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * Determine the charset of the response (for inclusion in a status exception).
	 * @param response the response to inspect
	 * @return the associated charset, or {@code null} if none
	 * @since 4.3.8
	 */
	protected Charset getCharset(ClientHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}

	/**
	 * Read the body of the given response (for inclusion in a status exception).
	 * @param response the response to inspect
	 * @return the response body as a byte array,
	 * or an empty byte array if the body could not be read
	 * @since 4.3.8
	 */
	protected byte[] getResponseBody(ClientHttpResponse response) {
		try {
			return FileCopyUtils.copyToByteArray(response.getBody());
		}
		catch (IOException ex) {
			// ignore
		}
		return new byte[0];
	}

}
