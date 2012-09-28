/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.test.web.mock.client.response;

import java.io.IOException;
import java.net.URI;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.mock.client.ResponseCreator;

/**
 * Static factory methods for obtaining a {@link ResponseCreator} instance.
 *
 * <p><strong>Eclipse users:</strong> consider adding this class as a Java editor
 * favorite. To navigate, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class ResponseCreators {


	private ResponseCreators() {
	}

	/**
	 * {@code ResponseCreator} for a 200 response (OK).
	 */
	public static DefaultResponseCreator withSuccess() {
		return new DefaultResponseCreator(HttpStatus.OK);
	}

	/**
	 * {@code ResponseCreator} for a 200 response (OK) with String body.
	 * @param body the response body, a "UTF-8" string
	 * @param mediaType the type of the content, may be {@code null}
	 */
	public static DefaultResponseCreator withSuccess(String body, MediaType mediaType) {
		return new DefaultResponseCreator(HttpStatus.OK).body(body).contentType(mediaType);
	}

	/**
	 * {@code ResponseCreator} for a 200 response (OK) with byte[] body.
	 * @param body the response body
	 * @param mediaType the type of the content, may be {@code null}
	 */
	public static DefaultResponseCreator withSuccess(byte[] body, MediaType contentType) {
		return new DefaultResponseCreator(HttpStatus.OK).body(body).contentType(contentType);
	}

	/**
	 * {@code ResponseCreator} for a 200 response (OK) content with {@link Resource}-based body.
	 * @param body the response body
	 * @param mediaType the type of the content, may be {@code null}
	 */
	public static DefaultResponseCreator withSuccess(Resource body, MediaType contentType) {
		return new DefaultResponseCreator(HttpStatus.OK).body(body).contentType(contentType);
	}

	/**
	 * {@code ResponseCreator} for a 201 response (CREATED) with a 'Location' header.
	 * @param location the value for the {@code Location} header
	 */
	public static DefaultResponseCreator withCreatedEntity(URI location) {
		return new DefaultResponseCreator(HttpStatus.CREATED).location(location);
	}

	/**
	 * {@code ResponseCreator} for a 204 response (NO_CONTENT).
	 */
	public static DefaultResponseCreator withNoContent() {
		return new DefaultResponseCreator(HttpStatus.NO_CONTENT);
	}

	/**
	 * {@code ResponseCreator} for a 400 response (BAD_REQUEST).
	 */
	public static DefaultResponseCreator withBadRequest() {
		return new DefaultResponseCreator(HttpStatus.BAD_REQUEST);
	}

	/**
	 * {@code ResponseCreator} for a 401 response (UNAUTHORIZED).
	 */
	public static DefaultResponseCreator withUnauthorizedRequest() {
		return new DefaultResponseCreator(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * {@code ResponseCreator} for a 500 response (SERVER_ERROR).
	 */
	public static DefaultResponseCreator withServerError() {
		return new DefaultResponseCreator(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * {@code ResponseCreator} with a specific HTTP status.
	 * @param status the response status
	 */
	public static DefaultResponseCreator withStatus(HttpStatus status) {
		return new DefaultResponseCreator(status);
	}

	/**
	 * Respond with a given body, headers, status code, and status text.
	 *
	 * @param body the body of the response "UTF-8" encoded
	 * @param headers the response headers
	 * @param statusCode the response status code
	 * @param statusText the response status text
	 *
	 * @deprecated in favor of methods returning DefaultResponseCreator
	 */
	public static ResponseCreator withResponse(final String body, final HttpHeaders headers,
			final HttpStatus statusCode, final String statusText) {

		return new ResponseCreator() {
			public MockClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
				MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes("UTF-8"), statusCode);
				response.getHeaders().putAll(headers);
				return response;
			}
		};
	}

	/**
	 * Respond with the given body, headers, and a status code of 200 (OK).
	 *
	 * @param body the body of the response "UTF-8" encoded
	 * @param headers the response headers
	 *
	 * @deprecated in favor of methods 'withXyz' in this class returning DefaultResponseCreator
	 */
	public static ResponseCreator withResponse(String body, HttpHeaders headers) {
		return withResponse(body, headers, HttpStatus.OK, "");
	}

	/**
	 * Respond with a given body, headers, status code, and text.
	 *
	 * @param body a {@link Resource} containing the body of the response
	 * @param headers the response headers
	 * @param statusCode the response status code
	 * @param statusText the response status text
	 *
	 * @deprecated in favor of methods 'withXyz' in this class returning DefaultResponseCreator
	 */
	public static ResponseCreator withResponse(final Resource body, final HttpHeaders headers,
			final HttpStatus statusCode, String statusText) {

		return new ResponseCreator() {
			public MockClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
				MockClientHttpResponse response = new MockClientHttpResponse(body.getInputStream(), statusCode);
				response.getHeaders().putAll(headers);
				return response;
			}
		};
	}

	/**
	 * Respond with the given body, headers, and a status code of 200 (OK).
	 * @param body the body of the response
	 * @param headers the response headers
	 *
	 * @deprecated in favor of methods 'withXyz' in this class returning DefaultResponseCreator
	 */
	public static ResponseCreator withResponse(final Resource body, final HttpHeaders headers) {
		return withResponse(body, headers, HttpStatus.OK, "");
	}

}
