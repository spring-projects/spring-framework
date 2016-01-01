/*
 * Copyright 2002-2015 the original author or authors.
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

/**
 * A contract for matching requests to expectations.
 *
 * @author Craig Walls
 * @since 3.2
 */
public interface RequestMatcher {

	/**
	 * Match the given request against some expectations.
	 * @param request the request to make assertions on
	 * @throws IOException in case of I/O errors
	 * @throws AssertionError if expectations are not met
	 */
	void match(ClientHttpRequest request) throws IOException, AssertionError;

}
