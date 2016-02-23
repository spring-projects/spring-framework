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
import java.util.Iterator;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@code RequestExpectationManager} that expects requests to follow the order
 * in which expected requests were declared.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class SimpleRequestExpectationManager extends AbstractRequestExpectationManager {

	private Iterator<RequestExpectation> iterator;


	@Override
	public ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		if (this.iterator == null) {
			this.iterator = getExpectations().iterator();
		}
		if (!this.iterator.hasNext()) {
			HttpMethod method = request.getMethod();
			URI uri = request.getURI();
			throw new AssertionError("No further requests expected: HTTP " + method + " " + uri);
		}
		RequestExpectation expectation = this.iterator.next();
		expectation.match(request);
		return expectation.createResponse(request);
	}

	@Override
	public void verify() {
		if (getExpectations().isEmpty() || getExpectations().size() == getRequests().size()) {
			return;
		}
		throw new AssertionError(getVerifyMessage());
	}

	private String getVerifyMessage() {
		StringBuilder sb = new StringBuilder("Further request(s) expected\n");
		if (getRequests().size() > 0) {
			sb.append("The following ");
		}
		sb.append(getRequests().size()).append(" out of ");
		sb.append(getExpectations().size()).append(" were executed");

		if (getRequests().size() > 0) {
			sb.append(":\n");
			for (ClientHttpRequest request : getRequests()) {
				sb.append(request.toString()).append("\n");
			}
		}
		return sb.toString();
	}

}
