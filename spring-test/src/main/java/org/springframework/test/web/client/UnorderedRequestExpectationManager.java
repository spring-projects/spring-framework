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
 * {@code RequestExpectationManager} that matches requests to expectations
 * regardless of the order of declaration of expectated requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class UnorderedRequestExpectationManager extends AbstractRequestExpectationManager {

	private final RequestExpectationGroup remainingExpectations = new RequestExpectationGroup();


	@Override
	protected void afterExpectationsDeclared() {
		this.remainingExpectations.updateAll(getExpectations());
	}

	@Override
	public ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation = this.remainingExpectations.findExpectation(request);
		if (expectation != null) {
			ClientHttpResponse response = expectation.createResponse(request);
			this.remainingExpectations.update(expectation);
			return response;
		}
		throw createUnexpectedRequestError(request);
	}

}
