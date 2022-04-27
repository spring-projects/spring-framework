/*
 * Copyright 2002-2021 the original author or authors.
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
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

	private final List<RequestExpectation> expectations = new ArrayList<>();

	private final List<ClientHttpRequest> requests = new ArrayList<>();

	private final Map<ClientHttpRequest, Throwable> requestFailures = new LinkedHashMap<>();


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
		RequestExpectation expectation;
		synchronized (this.requests) {
			if (this.requests.isEmpty()) {
				afterExpectationsDeclared();
			}
			try {
				// Try this first for backwards compatibility
				ClientHttpResponse response = validateRequestInternal(request);
				if (response != null) {
					return response;
				}
				else {
					expectation = matchRequest(request);
				}
			}
			catch (Throwable ex) {
				this.requestFailures.put(request, ex);
				throw ex;
			}
			finally {
				this.requests.add(request);
			}
		}
		return expectation.createResponse(request);
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
	 * @deprecated as of 5.0.3, subclasses should implement {@link #matchRequest(ClientHttpRequest)}
	 * instead and return only the matched expectation, leaving the call to create the response
	 * as a separate step (to be invoked by this class).
	 */
	@Deprecated
	@Nullable
	protected ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		return null;
	}

	/**
	 * As of 5.0.3 subclasses should implement this method instead of
	 * {@link #validateRequestInternal(ClientHttpRequest)} in order to match the
	 * request to an expectation, leaving the call to create the response as a separate step
	 * (to be invoked by this class).
	 * @param request the current request
	 * @return the matched expectation with its request count updated via
	 * {@link RequestExpectation#incrementAndValidate()}.
	 * @since 5.0.3
	 */
	protected RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
		throw new UnsupportedOperationException(
				"It looks like neither the deprecated \"validateRequestInternal\"" +
				"nor its replacement (this method) are implemented.");
	}

	@Override
	public void verify() {
		int count = verifyInternal();
		if (count > 0) {
			String message = "Further request(s) expected leaving " + count + " unsatisfied expectation(s).\n";
			throw new AssertionError(message + getRequestDetails());
		}
	}

	@Override
	public void verify(Duration timeout) {
		Instant endTime = Instant.now().plus(timeout);
		do {
			if (verifyInternal() == 0) {
				return;
			}
		}
		while (Instant.now().isBefore(endTime));
		verify();
	}

	private int verifyInternal() {
		if (this.expectations.isEmpty()) {
			return 0;
		}
		if (!this.requestFailures.isEmpty()) {
			throw new AssertionError("Some requests did not execute successfully.\n" +
					this.requestFailures.entrySet().stream()
							.map(entry -> "Failed request:\n" + entry.getKey() + "\n" + entry.getValue())
							.collect(Collectors.joining("\n", "\n", "")));
		}
		int count = 0;
		for (RequestExpectation expectation : this.expectations) {
			if (!expectation.isSatisfied()) {
				count++;
			}
		}
		return count;
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
				sb.append(request.toString()).append('\n');
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
		this.requestFailures.clear();
	}


	/**
	 * Helper class to manage a group of remaining expectations.
	 */
	protected static class RequestExpectationGroup {

		private final Set<RequestExpectation> expectations = new LinkedHashSet<>();

		public void addAllExpectations(Collection<RequestExpectation> expectations) {
			this.expectations.addAll(expectations);
		}

		public Set<RequestExpectation> getExpectations() {
			return this.expectations;
		}

		/**
		 * Return a matching expectation, or {@code null} if none match.
		 */
		@Nullable
		public RequestExpectation findExpectation(ClientHttpRequest request) throws IOException {
			for (RequestExpectation expectation : this.expectations) {
				try {
					expectation.match(request);
					return expectation;
				}
				catch (AssertionError error) {
					// We're looking to find a match or return null..
				}
			}
			return null;
		}

		/**
		 * Invoke this for an expectation that has been matched.
		 * <p>The count of the given expectation is incremented, then it is
		 * either stored if remainingCount &gt; 0 or removed otherwise.
		 */
		public void update(RequestExpectation expectation) {
			expectation.incrementAndValidate();
			updateInternal(expectation);
		}

		private void updateInternal(RequestExpectation expectation) {
			if (expectation.hasRemainingCount()) {
				this.expectations.add(expectation);
			}
			else {
				this.expectations.remove(expectation);
			}
		}

		/**
		 * Add expectations to this group.
		 * @deprecated as of 5.0.3, if favor of {@link #addAllExpectations}
		 */
		@Deprecated
		public void updateAll(Collection<RequestExpectation> expectations) {
			expectations.forEach(this::updateInternal);
		}

		/**
		 * Reset all expectations for this group.
		 */
		public void reset() {
			this.expectations.clear();
		}
	}

}
