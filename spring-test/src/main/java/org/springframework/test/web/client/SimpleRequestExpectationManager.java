/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client;

import java.io.IOException;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.util.Assert;

/**
 * Simple {@code RequestExpectationManager} that matches requests to expectations
 * sequentially, i.e. in the order of declaration of expectations.
 *
 * <p>When request expectations have an expected count greater than one,
 * only the first execution is expected to match the order of declaration.
 * Subsequent request executions may be inserted anywhere thereafter.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.3
 */
public class SimpleRequestExpectationManager extends AbstractRequestExpectationManager {

	/** Expectations in the order of declaration (count may be > 1). */
	private @Nullable Iterator<RequestExpectation> expectationIterator;

	/** Track expectations that have a remaining count. */
	private final RequestExpectationGroup repeatExpectations = new RequestExpectationGroup();


	@Override
	protected void afterExpectationsDeclared() {
		Assert.state(this.expectationIterator == null, "Expectations already declared");
		this.expectationIterator = getExpectations().iterator();
	}

	@Override
	protected RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation = this.repeatExpectations.findExpectation(request);
		if (expectation == null) {
			if (this.expectationIterator == null || !this.expectationIterator.hasNext()) {
				throw createUnexpectedRequestError(request);
			}
			expectation = this.expectationIterator.next();
			expectation.match(request);
		}
		this.repeatExpectations.update(expectation);
		return expectation;
	}

	@Override
	public void reset() {
		super.reset();
		this.expectationIterator = null;
		this.repeatExpectations.reset();
	}

}
