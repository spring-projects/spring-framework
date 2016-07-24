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
import java.util.Iterator;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
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
 * @since 4.3
 */
public class SimpleRequestExpectationManager extends AbstractRequestExpectationManager {

	private Iterator<RequestExpectation> expectationIterator;

	private final RequestExpectationGroup repeatExpectations = new RequestExpectationGroup();


	@Override
	protected void afterExpectationsDeclared() {
		Assert.state(this.expectationIterator == null);
		this.expectationIterator = getExpectations().iterator();
	}

	@Override
	public ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation;
		try {
			expectation = next(request);
			expectation.match(request);
		}
		catch (AssertionError error) {
			expectation = this.repeatExpectations.findExpectation(request);
			if (expectation == null) {
				throw error;
			}
		}
		ClientHttpResponse response = expectation.createResponse(request);
		this.repeatExpectations.update(expectation);
		return response;
	}

	private RequestExpectation next(ClientHttpRequest request) {
		if (this.expectationIterator.hasNext()) {
			return this.expectationIterator.next();
		}
		throw createUnexpectedRequestError(request);
	}

	@Override
	public void reset() {
		super.reset();
		this.expectationIterator = null;
		this.repeatExpectations.reset();
	}

}
