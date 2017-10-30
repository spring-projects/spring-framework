/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for {@code RequestExpectationManager} implementations responsible
 * for storing expectations and actual requests, and checking for unsatisfied
 * expectations at the end.
 *
 * <p>Subclasses are responsible for validating each request by matching it to
 * to expectations following the order of declaration or not.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.3
 */
public abstract class AbstractRequestExpectationManager implements RequestExpectationManager {

	private final List<RequestExpectation> expectations = new LinkedList<>();

	private final List<ClientHttpRequest> requests = new LinkedList<>();


	/**
	 * Return a read-only list of the expectations.
	 */
	protected List<RequestExpectation> getExpectations() {
		return Collections.unmodifiableList(this.expectations);
	}

	/**
	 * Return a read-only list of requests executed so far.
	 */
	protected List<ClientHttpRequest> getRequests() {
		return Collections.unmodifiableList(this.requests);
	}


	@Override
	public ResponseActions expectRequest(ExpectedCount count, RequestMatcher matcher) {
		Assert.state(this.requests.isEmpty(), "Cannot add more expectations after actual requests are made");
		RequestExpectation expectation = new DefaultRequestExpectation(count, matcher);
		this.expectations.add(expectation);
		return expectation;
	}

	@Override
	public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
		synchronized (this.requests) {
			if (this.requests.isEmpty()) {
				afterExpectationsDeclared();
			}
			try {
				return validateRequestInternal(request);
			}
			finally {
				this.requests.add(request);
			}
		}
	}

	/**
	 * Invoked at the time of the first actual request, which effectively means
	 * the expectations declaration phase is over.
	 */
	protected void afterExpectationsDeclared() {
	}

	/**
	 * Subclasses must implement the actual validation of the request
	 * matching to declared expectations.
	 */
	protected abstract ClientHttpResponse validateRequestInternal(ClientHttpRequest request)
			throws IOException;

	@Override
	public void verify() {
		if (this.expectations.isEmpty()) {
			return;
		}
		int count = 0;
		for (RequestExpectation expectation : this.expectations) {
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
		sb.append(this.requests.size()).append(" request(s) executed");
		if (!this.requests.isEmpty()) {
			sb.append(":\n");
			for (ClientHttpRequest request : this.requests) {
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

	@Override
	public void reset() {
		this.expectations.clear();
		this.requests.clear();
	}


	/**
	 * Helper class to manage a group of remaining expectations.
	 */
	protected static class RequestExpectationGroup {

		private final Set<RequestExpectation> expectations = new LinkedHashSet<>();

		public Set<RequestExpectation> getExpectations() {
			return this.expectations;
		}


		@Nullable
		public RequestExpectation findExpectation(ClientHttpRequest request) throws IOException {
			for (RequestExpectation expectation : this.expectations) {
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

		/**
		 * Invoke this for an expectation that has been matched.
		 * <p>The given expectation will either be stored if it has a remaining
		 * count or it will be removed otherwise.
		 */
		public void update(RequestExpectation expectation) {
			if (expectation.hasRemainingCount()) {
				this.expectations.add(expectation);
			}
			else {
				this.expectations.remove(expectation);
			}
		}

		/**
		 * Collection variant of {@link #update(RequestExpectation)} that can
		 * be used to insert expectations.
		 */
		public void updateAll(Collection<RequestExpectation> expectations) {
			expectations.forEach(this::update);
		}

		public void reset() {
			this.expectations.clear();
		}
	}

}
