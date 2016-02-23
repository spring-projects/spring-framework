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
import java.util.LinkedList;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@code RequestExpectationManager} that matches requests to expectations
 * regardless of the order of declaration of expectations.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class UnorderedRequestExpectationManager extends AbstractRequestExpectationManager {

	private final List<RequestExpectation> remainingExpectations = new LinkedList<RequestExpectation>();


	protected List<RequestExpectation> getRemainingExpectations() {
		return this.remainingExpectations;
	}


	@Override
	public ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		if (getRequests().isEmpty()) {
			getRemainingExpectations().addAll(getExpectations());
		}
		for (RequestExpectation expectation : getExpectations()) {
			try {
				expectation.match(request);
				getRemainingExpectations().remove(expectation);
				return expectation.createResponse(request);
			}
			catch (AssertionError error) {
				// Ignore
			}
		}
		HttpMethod method = request.getMethod();
		URI uri = request.getURI();
		throw new AssertionError("Unexpected request: HTTP " + method + " " + uri);
	}

	@Override
	public void verify() {
		if (getExpectations().isEmpty() || this.remainingExpectations.isEmpty()) {
			return;
		}
		throw new AssertionError(getVerifyMessage());
	}

	private String getVerifyMessage() {
		StringBuilder sb = new StringBuilder("Further request(s) expected\n");
		if (getRequests().size() > 0) {
			sb.append("The following ");
		}
		sb.append(getRequests().size()).append(" were executed");
		sb.append(" leaving ").append(this.remainingExpectations.size()).append(" expectations.");

		if (getRequests().size() > 0) {
			sb.append(":\n");
			for (ClientHttpRequest request : getRequests()) {
				sb.append(request.toString()).append("\n");
			}
		}
		return sb.toString();
	}

}
