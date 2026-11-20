/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;

/**
 * {@code RequestExpectationManager} that matches requests to expectations
 * regardless of the order of declaration of expected requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class UnorderedRequestExpectationManager extends AbstractRequestExpectationManager {

	private final RequestExpectationGroup remainingExpectations = new RequestExpectationGroup();


	@Override
	protected void afterExpectationsDeclared() {
		this.remainingExpectations.addAllExpectations(getExpectations());
	}

	@Override
	public RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation = this.remainingExpectations.findExpectation(request);
		if (expectation == null) {
			throw createUnexpectedRequestError(request);
		}
		this.remainingExpectations.update(expectation);
		return expectation;
	}

	@Override
	public void reset() {
		super.reset();
		this.remainingExpectations.reset();
	}

}
