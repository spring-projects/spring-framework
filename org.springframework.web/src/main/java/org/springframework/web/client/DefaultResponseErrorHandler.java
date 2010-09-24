/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;

/**
 * Default implementation of the {@link ResponseErrorHandler} interface.
 *
 * <p>This error handler checks for the status code on the {@link ClientHttpResponse}: any code with series
 * {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR} or
 * {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR} is considered to be an error.
 * This behavior can be changed by overriding the {@link #hasError(HttpStatus)} method.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see RestTemplate#setErrorHandler
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

	/**
	 * Delegates to {@link #hasError(HttpStatus)} with the response status code.
	 */
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return hasError(response.getStatusCode());
	}

	/**
	 * Template method called from {@link #hasError(ClientHttpResponse)}.
	 * <p>The default implementation checks if the given status code is
	 * {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR CLIENT_ERROR}
	 * or {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR SERVER_ERROR}. Can be overridden in subclasses.
	 * @param statusCode the HTTP status code
	 * @return <code>true</code> if the response has an error; <code>false</code> otherwise
	 */
	protected boolean hasError(HttpStatus statusCode) {
		return (statusCode.series() == HttpStatus.Series.CLIENT_ERROR ||
				statusCode.series() == HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * {@inheritDoc}
	 * <p>The default implementation throws a {@link HttpClientErrorException} if the response status code is
	 * {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR}, a {@link HttpServerErrorException} if it is
	 * {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR}, and a {@link RestClientException} in other
	 * cases.
	 */
	public void handleError(ClientHttpResponse response) throws IOException {
		HttpStatus statusCode = response.getStatusCode();
		MediaType contentType = response.getHeaders().getContentType();
		Charset charset = contentType != null ? contentType.getCharSet() : null;
		byte[] body = FileCopyUtils.copyToByteArray(response.getBody());
		switch (statusCode.series()) {
			case CLIENT_ERROR:
				throw new HttpClientErrorException(statusCode, response.getStatusText(), body, charset);
			case SERVER_ERROR:
				throw new HttpServerErrorException(statusCode, response.getStatusText(), body, charset);
			default:
				throw new RestClientException("Unknown status code [" + statusCode + "]");
		}
	}

}

