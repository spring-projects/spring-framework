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
package org.springframework.test.web.client;

/**
 * A contract for setting up request expectations and defining a response.
 * Implementations can be obtained through {@link MockRestServiceServer#expect(RequestMatcher)}.
 *
 * @author Craig Walls
 * @since 3.2
 */
public interface ResponseActions {

	/**
	 * Add a request expectation.
	 * @return the expectation
	 */
	ResponseActions andExpect(RequestMatcher requestMatcher);

	/**
	 * Define the response.
	 * @param responseCreator the creator of the response
	 */
	void andRespond(ResponseCreator responseCreator);

}
