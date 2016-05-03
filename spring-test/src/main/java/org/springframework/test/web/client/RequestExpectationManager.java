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
package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Abstraction for creating HTTP request expectations, applying them to actual
 * requests (in strict or random order), and verifying whether expectations
 * have been met.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public interface RequestExpectationManager {

	/**
	 * Set up a new request expectation. The returned {@link ResponseActions} is
	 * used to add more expectations and define a response.
	 * @param requestMatcher a request expectation
	 * @return for setting up further expectations and define a response
	 */
	ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher);

	/**
	 * Validate the given actual request against the declared expectations.
	 * Is successful return the mock response to use or raise an error.
	 * @param request the request
	 * @return the response to return if the request was validated.
	 * @throws AssertionError when some expectations were not met
	 * @throws IOException
	 */
	ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException;

	/**
	 * Verify that all expectations have been met.
	 * @throws AssertionError when some expectations were not met
	 */
	void verify();

}
