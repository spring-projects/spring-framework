/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.util.Assert;

/**
 * A specialization of {@code MockClientHttpRequest} that matches the request
 * against a set of expectations, via {@link RequestMatcher} instances. The
 * expectations are checked when the request is executed. This class also uses a
 * {@link ResponseCreator} to create the response.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class RequestMatcherClientHttpRequest extends MockClientHttpRequest implements ResponseActions {

	private final List<RequestMatcher> requestMatchers = new LinkedList<RequestMatcher>();

	private ResponseCreator responseCreator;


	public RequestMatcherClientHttpRequest(RequestMatcher requestMatcher) {
		Assert.notNull(requestMatcher, "RequestMatcher is required");
		this.requestMatchers.add(requestMatcher);
	}

	public ResponseActions andExpect(RequestMatcher requestMatcher) {
		Assert.notNull(requestMatcher, "RequestMatcher is required");
		this.requestMatchers.add(requestMatcher);
		return this;
	}

	public void andRespond(ResponseCreator responseCreator) {
		Assert.notNull(responseCreator, "ResponseCreator is required");
		this.responseCreator = responseCreator;
	}

	public ClientHttpResponse execute() throws IOException {

		if (this.requestMatchers.isEmpty()) {
			throw new AssertionError("No request expectations to execute");
		}

		if (this.responseCreator == null) {
			throw new AssertionError("No ResponseCreator was set up. Add it after request expectations, "
					+ "e.g. MockRestServiceServer.expect(requestTo(\"/foo\")).andRespond(withSuccess())");
		}

		for (RequestMatcher requestMatcher : this.requestMatchers) {
			requestMatcher.match(this);
		}

		setResponse(this.responseCreator.createResponse(this));

		return super.execute();
	}

}
