/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client;

/**
 * An extension of {@code ResponseActions} that also implements
 * {@code RequestMatcher} and {@code ResponseCreator}
 *
 * <p>While {@code ResponseActions} is the API for defining expectations this
 * sub-interface is the internal SPI for matching these expectations to actual
 * requests and for creating responses.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public interface RequestExpectation extends ResponseActions, RequestMatcher, ResponseCreator {

	/**
	 * Whether there is a remaining count of invocations for this expectation.
	 */
	boolean hasRemainingCount();

	/**
	 * Increase the matched request count and check we haven't passed the max count.
	 * @since 5.0.3
	 */
	void incrementAndValidate();

	/**
	 * Whether the requirements for this request expectation have been met.
	 */
	boolean isSatisfied();

}
