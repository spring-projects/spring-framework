/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

/**
 * Represents a reactive server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

	/**
	 * Set the HTTP status code of the response.
	 * @param status the HTTP status as an {@link HttpStatus} enum value
	 * @return {@code false} if the status code has not been set because the HTTP response
	 * is already committed, {@code true} if it has been set correctly.
	 */
	boolean setStatusCode(HttpStatus status);

	/**
	 * Return the HTTP status code or {@code null} if not set.
	 */
	HttpStatus getStatusCode();

	/**
	 * Return a mutable map with the cookies to send to the server.
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

	/**
	 * A mechanism for URL rewriting that applications and libraries such as
	 * HTML template libraries to use consistently for all URLs emitted by
	 * the application. Doing so enables the registration of URL encoders via
	 * {@link #registerUrlEncoder} that can insert an id for authentication,
	 * a nonce for CSRF protection, a version for a static resource, etc.
	 * @param url the URL to encode
	 * @return the encoded URL or the same
	 */
	String encodeUrl(String url);

	/**
	 * Register a URL rewriting function for use with {@link #encodeUrl}.
	 * The function must return an encoded URL or the same URL.
	 * @param encoder a URL encoding function to use
	 */
	void registerUrlEncoder(Function<String, String> encoder);

	/**
	 * Indicate that request handling is complete, allowing for any cleanup or
	 * end-of-processing tasks to be performed such as applying header changes
	 * made via {@link #getHeaders()} to the underlying server response (if not
	 * applied already).
	 * <p>This method should be automatically invoked at the end of request
	 * processing so typically applications should not have to invoke it.
	 * If invoked multiple times it should have no side effects.
	 */
	Mono<Void> setComplete();

}
