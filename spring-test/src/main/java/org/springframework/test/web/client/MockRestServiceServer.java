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

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockAsyncClientHttpRequest;
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
 * <p>An alternative to the above is to use
 * {@link MockMvcClientHttpRequestFactory} which allows executing requests
 * against a {@link org.springframework.test.web.servlet.MockMvc MockMvc}
 * instance. That allows you to process requests using your server-side code
 * but without running a server.
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

	private final RequestExpectationManager expectationManager;


	/**
	 * Private constructor.
	 * See static builder methods and {@code createServer} shortcut methods.
	 */
	private MockRestServiceServer() {
		this.expectationManager = new SimpleRequestExpectationManager();
	}

	/**
	 * Private constructor with {@code RequestExpectationManager}.
	 * See static builder methods and {@code createServer} shortcut methods.
	 */
	private MockRestServiceServer(RequestExpectationManager expectationManager) {
		this.expectationManager = expectationManager;
	}


	/**
	 * Set up a new HTTP request expectation. The returned {@link ResponseActions}
	 * is used to set up further expectations and to define the response.
	 * <p>This method may be invoked multiple times before starting the test, i.e. before
	 * using the {@code RestTemplate}, to set up expectations for multiple requests.
	 * @param matcher a request expectation, see {@link MockRestRequestMatchers}
	 * @return used to set up further expectations or to define a response
	 */
	public ResponseActions expect(RequestMatcher matcher) {
		return this.expectationManager.expectRequest(matcher);
	}


	/**
	 * Verify that all expected requests set up via
	 * {@link #expect(RequestMatcher)} were indeed performed.
	 * @throws AssertionError when some expectations were not met
	 */
	public void verify() {
		this.expectationManager.verify();
	}


	/**
	 * Build a {@code MockRestServiceServer} with a {@code RestTemplate}.
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder restTemplate(RestTemplate restTemplate) {
		return new DefaultBuilder(restTemplate);
	}

	/**
	 * Build a {@code MockRestServiceServer} with an {@code AsyncRestTemplate}.
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder asyncRestTemplate(AsyncRestTemplate asyncRestTemplate) {
		return new DefaultBuilder(asyncRestTemplate);
	}

	/**
	 * Build a {@code MockRestServiceServer} with a {@code RestGateway}.
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder restGateway(RestGatewaySupport restGateway) {
		Assert.notNull(restGateway, "'gatewaySupport' must not be null");
		return new DefaultBuilder(restGateway.getRestTemplate());
	}


	/**
	 * A shortcut for {@code restTemplate(restTemplate).build()}.
	 * @param restTemplate the RestTemplate to set up for mock testing
	 * @return the mock server
	 */
	public static MockRestServiceServer createServer(RestTemplate restTemplate) {
		return restTemplate(restTemplate).build();
	}

	/**
	 * A shortcut for {@code asyncRestTemplate(asyncRestTemplate).build()}.
	 * @param asyncRestTemplate the AsyncRestTemplate to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(AsyncRestTemplate asyncRestTemplate) {
		return asyncRestTemplate(asyncRestTemplate).build();
	}

	/**
	 * A shortcut for {@code restGateway(restGateway).build()}.
	 * @param restGateway the REST gateway to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(RestGatewaySupport restGateway) {
		return restGateway(restGateway).build();
	}



	/**
	 * Builder to create a {@code MockRestServiceServer}.

	 */
	public interface MockRestServiceServerBuilder {

		/**
		 * Allow expected requests to be executed in any order not necessarily
		 * matching the order of declaration. This is a shortcut for:<br>
		 * {@code builder.expectationManager(new UnorderedRequestExpectationManager)}
		 */
		MockRestServiceServerBuilder unordered();

		/**
		 * Configure a custom {@code RequestExpectationManager}.
		 * <p>By default {@link SimpleRequestExpectationManager} is used. It is
		 * also possible to switch to {@link UnorderedRequestExpectationManager}
		 * by setting {@link #unordered()}.
		 */
		MockRestServiceServerBuilder expectationManager(RequestExpectationManager manager);

		/**
		 * Build the {@code MockRestServiceServer} and setting up the underlying
		 * {@code RestTemplate} or {@code AsyncRestTemplate} with a
		 * {@link ClientHttpRequestFactory} that creates mock requests.
		 */
		MockRestServiceServer build();

	}

	private static class DefaultBuilder implements MockRestServiceServerBuilder {

		private final RestTemplate restTemplate;

		private final AsyncRestTemplate asyncRestTemplate;

		private RequestExpectationManager expectationManager = new SimpleRequestExpectationManager();


		public DefaultBuilder(RestTemplate restTemplate) {
			Assert.notNull(restTemplate, "'restTemplate' must not be null");
			this.restTemplate = restTemplate;
			this.asyncRestTemplate = null;
		}

		public DefaultBuilder(AsyncRestTemplate asyncRestTemplate) {
			Assert.notNull(asyncRestTemplate, "'asyncRestTemplate' must not be null");
			this.restTemplate = null;
			this.asyncRestTemplate = asyncRestTemplate;
		}


		@Override
		public MockRestServiceServerBuilder unordered() {
			expectationManager(new UnorderedRequestExpectationManager());
			return this;
		}

		@Override
		public MockRestServiceServerBuilder expectationManager(RequestExpectationManager manager) {
			Assert.notNull(manager, "'manager' is required.");
			this.expectationManager = manager;
			return this;
		}

		@Override
		public MockRestServiceServer build() {
			MockRestServiceServer server = new MockRestServiceServer(this.expectationManager);
			MockClientHttpRequestFactory factory = server.new MockClientHttpRequestFactory();
			if (this.restTemplate != null) {
				this.restTemplate.setRequestFactory(factory);
			}
			if (this.asyncRestTemplate != null) {
				this.asyncRestTemplate.setAsyncRequestFactory(factory);
			}
			return server;
		}
	}


	/**
	 * Mock ClientHttpRequestFactory that creates requests by iterating
	 * over the list of expected {@link DefaultRequestExpectation}'s.
	 */
	private class MockClientHttpRequestFactory implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			return createRequestInternal(uri, httpMethod);
		}

		@Override
		public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
			return createRequestInternal(uri, httpMethod);
		}

		private MockAsyncClientHttpRequest createRequestInternal(URI uri, HttpMethod method) {
			Assert.notNull(uri, "'uri' must not be null");
			Assert.notNull(method, "'httpMethod' must not be null");

			return new MockAsyncClientHttpRequest(method, uri) {

				@Override
				protected ClientHttpResponse executeInternal() throws IOException {
					ClientHttpResponse response = expectationManager.validateRequest(this);
					setResponse(response);
					return response;
				}
			};
		}
	}

}
