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
import java.util.LinkedList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * Base class for {@code RequestExpectationManager} implementations.
 * Creates and contains expectations and stores actual requests.
 *
 * <p>Sub-classes are responsible for matching actual to expected requests and
 * for verifying remaining expectations at the end.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public abstract class AbstractRequestExpectationManager implements RequestExpectationManager {

	private final List<RequestExpectation> expectations = new LinkedList<RequestExpectation>();

	private final List<ClientHttpRequest> requests = new LinkedList<ClientHttpRequest>();


	public AbstractRequestExpectationManager() {
	}

	public AbstractRequestExpectationManager(List<RequestExpectation> expectations) {
		this.expectations.addAll(expectations);
	}


	public List<RequestExpectation> getExpectations() {
		return this.expectations;
	}

	public List<ClientHttpRequest> getRequests() {
		return this.requests;
	}

	@Override
	public ResponseActions expectRequest(RequestMatcher requestMatcher) {
		Assert.state(getRequests().isEmpty(), "Cannot add more expectations after actual requests are made.");
		DefaultResponseActions expectation = new DefaultResponseActions(requestMatcher);
		getExpectations().add(expectation);
		return expectation;
	}

	@Override
	public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
		ClientHttpResponse response = validateRequestInternal(request);
		getRequests().add(request);
		return response;
	}

	protected abstract ClientHttpResponse validateRequestInternal(ClientHttpRequest request)
			throws IOException;

}
