/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
