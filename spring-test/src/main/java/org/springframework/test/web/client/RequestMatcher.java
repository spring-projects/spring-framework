/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;

/**
 * A contract for matching requests to expectations.
 *
 * <p>See {@link org.springframework.test.web.client.match.MockRestRequestMatchers
 * MockRestRequestMatchers} for static factory methods.
 *
 * @author Craig Walls
 * @since 3.2
 */
@FunctionalInterface
public interface RequestMatcher {

	/**
	 * Match the given request against specific expectations.
	 * @param request the request to make assertions on
	 * @throws IOException in case of I/O errors
	 * @throws AssertionError if expectations are not met
	 */
	void match(ClientHttpRequest request) throws IOException, AssertionError;

}
