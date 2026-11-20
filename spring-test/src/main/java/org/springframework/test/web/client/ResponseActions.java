/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
