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
 * Default implementation of {@code RequestExpectation} that simply delegates
 * to the request matchers and the response creator it contains.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class DefaultRequestExpectation implements RequestExpectation {

	private final RequestCount requestCount;

	private final List<RequestMatcher> requestMatchers = new LinkedList<RequestMatcher>();

	private ResponseCreator responseCreator;


	/**
	 * Create a new request expectation that should be called a number of times
	 * as indicated by {@code RequestCount}.
	 * @param expectedCount the expected request expectedCount
	 */
	public DefaultRequestExpectation(ExpectedCount expectedCount, RequestMatcher requestMatcher) {
		Assert.notNull(expectedCount, "'expectedCount' is required");
		Assert.notNull(requestMatcher, "'requestMatcher' is required");
		this.requestCount = new RequestCount(expectedCount);
		this.requestMatchers.add(requestMatcher);
	}


	protected RequestCount getRequestCount() {
		return this.requestCount;
	}

	protected List<RequestMatcher> getRequestMatchers() {
		return this.requestMatchers;
	}

	protected ResponseCreator getResponseCreator() {
		return this.responseCreator;
	}

	@Override
	public ResponseActions andExpect(RequestMatcher requestMatcher) {
		Assert.notNull(requestMatcher, "RequestMatcher is required");
		this.requestMatchers.add(requestMatcher);
		return this;
	}

	@Override
	public void andRespond(ResponseCreator responseCreator) {
		Assert.notNull(responseCreator, "ResponseCreator is required");
		this.responseCreator = responseCreator;
	}

	@Override
	public void match(ClientHttpRequest request) throws IOException {
		for (RequestMatcher matcher : getRequestMatchers()) {
			matcher.match(request);
		}
	}

	@Override
	public ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
		if (getResponseCreator() == null) {
			throw new IllegalStateException("createResponse called before ResponseCreator was set.");
		}
		getRequestCount().incrementAndValidate();
		return getResponseCreator().createResponse(request);
	}

	@Override
	public boolean hasRemainingCount() {
		return getRequestCount().hasRemainingCount();
	}

	@Override
	public boolean isSatisfied() {
		return getRequestCount().isSatisfied();
	}


	/**
	 * Helper class that keeps track of actual vs expected request count.
	 */
	protected static class RequestCount {

		private final ExpectedCount expectedCount;

		private int matchedRequestCount;


		public RequestCount(ExpectedCount expectedCount) {
			this.expectedCount = expectedCount;
		}


		public ExpectedCount getExpectedCount() {
			return this.expectedCount;
		}

		public int getMatchedRequestCount() {
			return this.matchedRequestCount;
		}

		public void incrementAndValidate() {
			this.matchedRequestCount++;
			if (getMatchedRequestCount() > getExpectedCount().getMaxCount()) {
				throw new AssertionError("No more calls expected.");
			}
		}

		public boolean hasRemainingCount() {
			return (getMatchedRequestCount() < getExpectedCount().getMaxCount());
		}

		public boolean isSatisfied() {
			return (getMatchedRequestCount() >= getExpectedCount().getMinCount());
		}
	}

}
