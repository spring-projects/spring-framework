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
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * Base class for {@code RequestExpectationManager} implementations responsible
 * for storing expectations and actual requests, and checking for unsatisfied
 * expectations at the end.
 *
 * <p>Sub-classes are responsible for validating each request by matching it to
 * to expectations following the order of declaration or not.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public abstract class AbstractRequestExpectationManager implements RequestExpectationManager {

	private final List<RequestExpectation> expectations = new LinkedList<RequestExpectation>();

	private final List<ClientHttpRequest> requests = new LinkedList<ClientHttpRequest>();


	protected List<RequestExpectation> getExpectations() {
		return this.expectations;
	}

	protected List<ClientHttpRequest> getRequests() {
		return this.requests;
	}


	@Override
	public ResponseActions expectRequest(ExpectedCount count, RequestMatcher matcher) {
		Assert.state(getRequests().isEmpty(), "Cannot add more expectations after actual requests are made.");
		RequestExpectation expectation = new DefaultRequestExpectation(count, matcher);
		getExpectations().add(expectation);
		return expectation;
	}

	@Override
	public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
		if (getRequests().isEmpty()) {
			afterExpectationsDeclared();
		}
		ClientHttpResponse response = validateRequestInternal(request);
		getRequests().add(request);
		return response;
	}

	/**
	 * Invoked after the phase of declaring expected requests is over. This is
	 * detected from {@link #validateRequest} on the first actual request.
	 */
	protected void afterExpectationsDeclared() {
	}

	/**
	 * Sub-classes must implement the actual validation of the request
	 * matching it to a declared expectation.
	 */
	protected abstract ClientHttpResponse validateRequestInternal(ClientHttpRequest request)
			throws IOException;

	@Override
	public void verify() {
		if (getExpectations().isEmpty()) {
			return;
		}
		int count = 0;
		for (RequestExpectation expectation : getExpectations()) {
			if (!expectation.isSatisfied()) {
				count++;
			}
		}
		if (count > 0) {
			String message = "Further request(s) expected leaving " + count + " unsatisfied expectation(s).\n";
			throw new AssertionError(message + getRequestDetails());
		}
	}

	/**
	 * Return details of executed requests.
	 */
	protected String getRequestDetails() {
		StringBuilder sb = new StringBuilder();
		sb.append(getRequests().size()).append(" request(s) executed");
		if (!getRequests().isEmpty()) {
			sb.append(":\n");
			for (ClientHttpRequest request : getRequests()) {
				sb.append(request.toString()).append("\n");
			}
		}
		else {
			sb.append(".\n");
		}
		return sb.toString();
	}

	/**
	 * Return an {@code AssertionError} that a sub-class can raise for an
	 * unexpected request.
	 */
	protected AssertionError createUnexpectedRequestError(ClientHttpRequest request) {
		HttpMethod method = request.getMethod();
		URI uri = request.getURI();
		String message = "No further requests expected: HTTP " + method + " " + uri + "\n";
		return new AssertionError(message + getRequestDetails());
	}


	/**
	 * Helper class to manage a group of request expectations. It helps with
	 * operations against the entire group such as finding a match and updating
	 * (add or remove) based on expected request count.
	 */
	protected static class RequestExpectationGroup {

		private final Set<RequestExpectation> expectations = new LinkedHashSet<RequestExpectation>();


		public Set<RequestExpectation> getExpectations() {
			return this.expectations;
		}

		public void update(RequestExpectation expectation) {
			if (expectation.hasRemainingCount()) {
				getExpectations().add(expectation);
			}
			else {
				getExpectations().remove(expectation);
			}
		}

		public void updateAll(Collection<RequestExpectation> expectations) {
			for (RequestExpectation expectation : expectations) {
				update(expectation);
			}
		}

		public RequestExpectation findExpectation(ClientHttpRequest request) throws IOException {
			for (RequestExpectation expectation : getExpectations()) {
				try {
					expectation.match(request);
					return expectation;
				}
				catch (AssertionError error) {
					// Ignore
				}
			}
			return null;
		}
	}

}
