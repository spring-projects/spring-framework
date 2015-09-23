/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.LinkedList;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

/**
 * <strong>Main entry point for client-side REST testing</strong>. Used for tests
 * that involve direct or indirect (through client code) use of the
 * {@link RestTemplate}. Provides a way to set up fine-grained expectations
 * on the requests that will be performed through the {@code RestTemplate} and
 * a way to define the responses to send back removing the need for an
 * actual running server.
 *
 * <p>Below is an example:
 * <pre class="code">
 * import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
 * import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
 * import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
 *
 * ...
 *
 * RestTemplate restTemplate = new RestTemplate()
 * MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
 *
 * mockServer.expect(requestTo("/hotels/42")).andExpect(method(HttpMethod.GET))
 *     .andRespond(withSuccess("{ \"id\" : \"42\", \"name\" : \"Holiday Inn\"}", MediaType.APPLICATION_JSON));
 *
 * Hotel hotel = restTemplate.getForObject("/hotels/{id}", Hotel.class, 42);
 * &#47;&#47; Use the hotel instance...
 *
 * mockServer.verify();
 * </pre>
 *
 * <p>To create an instance of this class, use {@link #createServer(RestTemplate)}
 * and provide the {@code RestTemplate} to set up for the mock testing.
 *
 * <p>After that use {@link #expect(RequestMatcher)} and fluent API methods
 * {@link ResponseActions#andExpect(RequestMatcher) andExpect(RequestMatcher)} and
 * {@link ResponseActions#andRespond(ResponseCreator) andRespond(ResponseCreator)}
 * to set up request expectations and responses, most likely relying on the default
 * {@code RequestMatcher} implementations provided in {@link MockRestRequestMatchers}
 * and the {@code ResponseCreator} implementations provided in
 * {@link MockRestResponseCreators} both of which can be statically imported.
 *
 * <p>At the end of the test use {@link #verify()} to ensure all expected
 * requests were actually performed.
 *
 * <p>Note that because of the fluent API offered by this class (and related
 * classes), you can typically use the Code Completion features (i.e.
 * ctrl-space) in your IDE to set up the mocks.
 *
 * <p><strong>Credits:</strong> The client-side REST testing support was
 * inspired by and initially based on similar code in the Spring WS project for
 * client-side tests involving the {@code WebServiceTemplate}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockRestServiceServer {

	private final List<RequestMatcherClientHttpRequest> expectedRequests =
			new LinkedList<RequestMatcherClientHttpRequest>();

	private final List<RequestMatcherClientHttpRequest> actualRequests =
			new LinkedList<RequestMatcherClientHttpRequest>();


	/**
	 * Private constructor.
	 * @see #createServer(RestTemplate)
	 * @see #createServer(RestGatewaySupport)
	 */
	private MockRestServiceServer() {
	}


	/**
	 * Create a {@code MockRestServiceServer} and set up the given
	 * {@code RestTemplate} with a mock {@link ClientHttpRequestFactory}.
	 * @param restTemplate the RestTemplate to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		MockRestServiceServer mockServer = new MockRestServiceServer();
		RequestMatcherClientHttpRequestFactory factory = mockServer.new RequestMatcherClientHttpRequestFactory();
		restTemplate.setRequestFactory(factory);
		return mockServer;
	}

	/**
	 * Create a {@code MockRestServiceServer} and set up the given
	 * {@code AsyRestTemplate} with a mock {@link AsyncClientHttpRequestFactory}.
	 * @param asyncRestTemplate the AsyncRestTemplate to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(AsyncRestTemplate asyncRestTemplate) {
		Assert.notNull(asyncRestTemplate, "'asyncRestTemplate' must not be null");
		MockRestServiceServer mockServer = new MockRestServiceServer();
		RequestMatcherClientHttpRequestFactory factory = mockServer.new RequestMatcherClientHttpRequestFactory();
		asyncRestTemplate.setAsyncRequestFactory(factory);
		return mockServer;
	}

	/**
	 * Create a {@code MockRestServiceServer} and set up the given
	 * {@code RestGatewaySupport} with a mock {@link ClientHttpRequestFactory}.
	 * @param restGateway the REST gateway to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(RestGatewaySupport restGateway) {
		Assert.notNull(restGateway, "'gatewaySupport' must not be null");
		return createServer(restGateway.getRestTemplate());
	}


	/**
	 * Set up a new HTTP request expectation. The returned {@link ResponseActions}
	 * is used to set up further expectations and to define the response.
	 * <p>This method may be invoked multiple times before starting the test, i.e. before
	 * using the {@code RestTemplate}, to set up expectations for multiple requests.
	 * @param requestMatcher a request expectation, see {@link MockRestRequestMatchers}
	 * @return used to set up further expectations or to define a response
	 */
	public ResponseActions expect(RequestMatcher requestMatcher) {
		Assert.state(this.actualRequests.isEmpty(), "Can't add more expected requests with test already underway");
		RequestMatcherClientHttpRequest request = new RequestMatcherClientHttpRequest(requestMatcher);
		this.expectedRequests.add(request);
		return request;
	}

	/**
	 * Verify that all expected requests set up via
	 * {@link #expect(RequestMatcher)} were indeed performed.
	 * @throws AssertionError when some expectations were not met
	 */
	public void verify() {
		if (this.expectedRequests.isEmpty() || this.expectedRequests.equals(this.actualRequests)) {
			return;
		}
		throw new AssertionError(getVerifyMessage());
	}

	private String getVerifyMessage() {
		StringBuilder sb = new StringBuilder("Further request(s) expected\n");
		if (this.actualRequests.size() > 0) {
			sb.append("The following ");
		}
		sb.append(this.actualRequests.size()).append(" out of ");
		sb.append(this.expectedRequests.size()).append(" were executed");

		if (this.actualRequests.size() > 0) {
			sb.append(":\n");
			for (RequestMatcherClientHttpRequest request : this.actualRequests) {
				sb.append(request.toString()).append("\n");
			}
		}
		return sb.toString();
	}


	/**
	 * Mock ClientHttpRequestFactory that creates requests by iterating
	 * over the list of expected {@link RequestMatcherClientHttpRequest}'s.
	 */
	private class RequestMatcherClientHttpRequestFactory
			implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {

		private Iterator<RequestMatcherClientHttpRequest> requestIterator;

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
			return createRequestInternal(uri, httpMethod);
		}

		@Override
		public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
			return createRequestInternal(uri, httpMethod);
		}

		private RequestMatcherClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
			Assert.notNull(uri, "'uri' must not be null");
			Assert.notNull(httpMethod, "'httpMethod' must not be null");

			if (this.requestIterator == null) {
				this.requestIterator = MockRestServiceServer.this.expectedRequests.iterator();
			}
			if (!this.requestIterator.hasNext()) {
				throw new AssertionError("No further requests expected: HTTP " + httpMethod + " " + uri);
			}

			RequestMatcherClientHttpRequest request = this.requestIterator.next();
			request.setURI(uri);
			request.setMethod(httpMethod);

			MockRestServiceServer.this.actualRequests.add(request);
			return request;
		}
	}

}
