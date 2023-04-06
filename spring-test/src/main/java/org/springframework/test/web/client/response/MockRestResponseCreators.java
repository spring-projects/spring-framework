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

package org.springframework.test.web.client.response;

import java.io.IOException;
import java.net.URI;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.web.client.ResponseCreator;

/**
 * Static factory methods to obtain a {@link ResponseCreator} with a fixed
 * response.
 *
 * <p>In addition, see also the {@link ExecutingResponseCreator} implementation
 * that performs actual requests to remote services.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @see ExecutingResponseCreator
 */
public abstract class MockRestResponseCreators {

	/**
	 * {@code ResponseCreator} for a 200 response (OK).
	 */
	public static DefaultResponseCreator withSuccess() {
		return new DefaultResponseCreator(HttpStatus.OK);
	}

	/**
	 * {@code ResponseCreator} for a 200 response (OK) with String body.
	 * @param body the response body, a "UTF-8" string
	 * @param contentType the type of the content (may be {@code null})
	 */
	public static DefaultResponseCreator withSuccess(String body, @Nullable MediaType contentType) {
		DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
		return (contentType != null ? creator.contentType(contentType) : creator);
	}

	/**
	 * {@code ResponseCreator} for a 200 response (OK) with byte[] body.
	 * @param body the response body
	 * @param contentType the type of the content (may be {@code null})
	 */
	public static DefaultResponseCreator withSuccess(byte[] body, @Nullable MediaType contentType) {
		DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
		return (contentType != null ? creator.contentType(contentType) : creator);
	}

	/**
	 * {@code ResponseCreator} for a 200 response (OK) content with {@link Resource}-based body.
	 * @param body the response body
	 * @param contentType the type of the content (may be {@code null})
	 */
	public static DefaultResponseCreator withSuccess(Resource body, @Nullable MediaType contentType) {
		DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
		return (contentType != null ? creator.contentType(contentType) : creator);
	}

	/**
	 * {@code ResponseCreator} for a 201 response (CREATED) with a 'Location' header.
	 * @param location the value for the {@code Location} header
	 */
	public static DefaultResponseCreator withCreatedEntity(URI location) {
		return new DefaultResponseCreator(HttpStatus.CREATED).location(location);
	}

	/**
	 * {@code ResponseCreator} for a 202 response (ACCEPTED).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withAccepted() {
		return new DefaultResponseCreator(HttpStatus.ACCEPTED);
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
	 * {@code ResponseCreator} for a 403 response (FORBIDDEN).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withForbiddenRequest() {
		return new DefaultResponseCreator(HttpStatus.FORBIDDEN);
	}

	/**
	 * {@code ResponseCreator} for a 404 response (NOT_FOUND).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withResourceNotFound() {
		return new DefaultResponseCreator(HttpStatus.NOT_FOUND);
	}

	/**
	 * {@code ResponseCreator} for a 409 response (CONFLICT).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withRequestConflict() {
		return new DefaultResponseCreator(HttpStatus.CONFLICT);
	}

	/**
	 * {@code ResponseCreator} for a 429 ratelimited response (TOO_MANY_REQUESTS).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withTooManyRequests() {
		return new DefaultResponseCreator(HttpStatus.TOO_MANY_REQUESTS);
	}

	/**
	 * {@code ResponseCreator} for a 429 rate-limited response (TOO_MANY_REQUESTS)
	 * with a {@code Retry-After} header in seconds.
	 * @since 6.0
	 */
	public static DefaultResponseCreator withTooManyRequests(int retryAfter) {
		return new DefaultResponseCreator(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfter));
	}

	/**
	 * {@code ResponseCreator} for a 500 response (SERVER_ERROR).
	 */
	public static DefaultResponseCreator withServerError() {
		return new DefaultResponseCreator(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * {@code ResponseCreator} for a 502 response (BAD_GATEWAY).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withBadGateway() {
		return new DefaultResponseCreator(HttpStatus.BAD_GATEWAY);
	}

	/**
	 * {@code ResponseCreator} for a 503 response (SERVICE_UNAVAILABLE).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withServiceUnavailable() {
		return new DefaultResponseCreator(HttpStatus.SERVICE_UNAVAILABLE);
	}

	/**
	 * {@code ResponseCreator} for a 504 response (GATEWAY_TIMEOUT).
	 * @since 6.0
	 */
	public static DefaultResponseCreator withGatewayTimeout() {
		return new DefaultResponseCreator(HttpStatus.GATEWAY_TIMEOUT);
	}

	/**
	 * {@code ResponseCreator} with a specific HTTP status.
	 * @param status the response status
	 */
	public static DefaultResponseCreator withStatus(HttpStatusCode status) {
		return new DefaultResponseCreator(status);
	}

	/**
	 * Variant of {@link #withStatus(HttpStatusCode)} with an integer.
	 * @param status the response status
	 * @since 5.3.17
	 */
	public static DefaultResponseCreator withRawStatus(int status) {
		return new DefaultResponseCreator(status);
	}

	/**
	 * {@code ResponseCreator} with an internal application {@code IOException}.
	 * <p>For example, one could use this to simulate a {@code SocketTimeoutException}.
	 * @param ex the {@code Exception} to be thrown at HTTP call time
	 * @since 5.2.2
	 */
	public static ResponseCreator withException(IOException ex) {
		return request -> {
			throw ex;
		};
	}

}
