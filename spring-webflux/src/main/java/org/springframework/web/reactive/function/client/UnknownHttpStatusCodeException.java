/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;

/**
 * Exception thrown when an unknown (or custom) HTTP status code is received.
 *
 * @author Brian Clozel
 * @since 5.1
 */
public class UnknownHttpStatusCodeException extends WebClientResponseException {

	private static final long serialVersionUID = 2407169540168185007L;


	/**
	 * Create a new instance of the {@code UnknownHttpStatusCodeException} with the given
	 * parameters.
	 */
	public UnknownHttpStatusCodeException(
			int statusCode, HttpHeaders headers, byte[] responseBody, Charset responseCharset) {

		super("Unknown status code [" + statusCode + "]", statusCode, "",
				headers, responseBody, responseCharset);
	}

	/**
	 * Create a new instance of the {@code UnknownHttpStatusCodeException} with the given
	 * parameters.
	 * @since 5.1.4
	 */
	public UnknownHttpStatusCodeException(
			int statusCode, HttpHeaders headers, byte[] responseBody, Charset responseCharset,
			@Nullable HttpRequest request) {

		super("Unknown status code [" + statusCode + "]", statusCode, "",
				headers, responseBody, responseCharset, request);
	}

}
