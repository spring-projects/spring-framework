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
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

/**
 * <strong>Main entry point for client-side REST testing</strong>. Used for tests
 * that involve direct or indirect use of the {@link RestTemplate}. Provides a
 * way to set up expected requests that will be performed through the
 * {@code RestTemplate} as well as mock responses to send back thus removing the
 * need for an actual server.
 *
 * <p>Below is an example that assumes static imports from
 * {@code MockRestRequestMatchers}, {@code MockRestResponseCreators},
 * and {@code ExpectedCount}:
 *
 * <pre class="code">
 * RestTemplate restTemplate = new RestTemplate()
 * MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
 *
 * server.expect(manyTimes(), requestTo("/hotels/42")).andExpect(method(HttpMethod.GET))
 *     .andRespond(withSuccess("{ \"id\" : \"42\", \"name\" : \"Holiday Inn\"}", MediaType.APPLICATION_JSON));
 *
 * Hotel hotel = restTemplate.getForObject("/hotels/{id}", Hotel.class, 42);
 * &#47;&#47; Use the hotel instance...
 *
 * // Verify all expectations met
 * server.verify();
 * </pre>
 *
 * <p>Note that as an alternative to the above you can also set the
 * {@link MockMvcClientHttpRequestFactory} on a {@code RestTemplate} which
 * allows executing requests against an instance of
 * {@link org.springframework.test.web.servlet.MockMvc MockMvc}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockRestServiceServer {

	private final RequestExpectationManager expectationManager;


	/**
	 * Private constructor with {@code RequestExpectationManager}.
	 * See static builder methods and {@code createServer} shortcut methods.
	 */
	private MockRestServiceServer(RequestExpectationManager expectationManager) {
		this.expectationManager = expectationManager;
	}


	/**
	 * Set up an expectation for a single HTTP request. The returned
	 * {@link ResponseActions} can be used to set up further expectations as
	 * well as to define the response.
	 *
	 * <p>This method may be invoked any number times before starting to make
	 * request through the underlying {@code RestTemplate} in order to set up
	 * all expected requests.
	 *
	 * @param matcher request matcher
	 * @return a representation of the expectation
	 */
	public ResponseActions expect(RequestMatcher matcher) {
		return expect(ExpectedCount.once(), matcher);
	}

	/**
	 * An alternative to {@link #expect(RequestMatcher)} with an indication how
	 * many times the request is expected to be executed.
	 *
	 * <p>When request expectations have an expected count greater than one, only
	 * the first execution is expected to match the order of declaration. Subsequent
	 * request executions may be inserted anywhere thereafter.
	 *
	 * @param count the expected count
	 * @param matcher request matcher
	 * @return a representation of the expectation
	 * @since 4.3
	 */
	public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
		return this.expectationManager.expectRequest(count, matcher);
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
	 * Reset the internal state removing all expectations and recorded requests.
	 */
	public void reset() {
		this.expectationManager.reset();
	}


	/**
	 * Return a builder for a {@code MockRestServiceServer} that should be used
	 * to reply to the given {@code RestTemplate}.
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder bindTo(RestTemplate restTemplate) {
		return new DefaultBuilder(restTemplate);
	}

	/**
	 * Return a builder for a {@code MockRestServiceServer} that should be used
	 * to reply to the given {@code AsyncRestTemplate}.
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder bindTo(AsyncRestTemplate asyncRestTemplate) {
		return new DefaultBuilder(asyncRestTemplate);
	}

	/**
	 * Return a builder for a {@code MockRestServiceServer} that should be used
	 * to reply to the given {@code RestGatewaySupport}.
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder bindTo(RestGatewaySupport restGateway) {
		Assert.notNull(restGateway, "'gatewaySupport' must not be null");
		return new DefaultBuilder(restGateway.getRestTemplate());
	}


	/**
	 * A shortcut for {@code bindTo(restTemplate).build()}.
	 * @param restTemplate the RestTemplate to set up for mock testing
	 * @return the mock server
	 */
	public static MockRestServiceServer createServer(RestTemplate restTemplate) {
		return bindTo(restTemplate).build();
	}

	/**
	 * A shortcut for {@code bindTo(asyncRestTemplate).build()}.
	 * @param asyncRestTemplate the AsyncRestTemplate to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(AsyncRestTemplate asyncRestTemplate) {
		return bindTo(asyncRestTemplate).build();
	}

	/**
	 * A shortcut for {@code bindTo(restGateway).build()}.
	 * @param restGateway the REST gateway to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(RestGatewaySupport restGateway) {
		return bindTo(restGateway).build();
	}


	/**
	 * Builder to create a {@code MockRestServiceServer}.
	 */
	public interface MockRestServiceServerBuilder {

		/**
		 * Whether to allow expected requests to be executed in any order not
		 * necessarily matching the order of declaration.
		 *
		 * <p>When set to "true" this is effectively a shortcut for:<br>
		 * {@code builder.build(new UnorderedRequestExpectationManager)}.
		 *
		 * @param ignoreExpectOrder whether to ignore the order of expectations
		 */
		MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder);

		/**
		 * Build the {@code MockRestServiceServer} and set up the underlying
		 * {@code RestTemplate} or {@code AsyncRestTemplate} with a
		 * {@link ClientHttpRequestFactory} that creates mock requests.
		 */
		MockRestServiceServer build();

		/**
		 * An overloaded build alternative that accepts a custom
		 * {@link RequestExpectationManager}.
		 */
		MockRestServiceServer build(RequestExpectationManager manager);

	}

	private static class DefaultBuilder implements MockRestServiceServerBuilder {

		private final RestTemplate restTemplate;

		private final AsyncRestTemplate asyncRestTemplate;

		private boolean ignoreExpectOrder;


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
		public MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder) {
			this.ignoreExpectOrder = ignoreExpectOrder;
			return this;
		}

		@Override
		public MockRestServiceServer build() {
			if (this.ignoreExpectOrder) {
				return build(new UnorderedRequestExpectationManager());
			}
			else {
				return build(new SimpleRequestExpectationManager());
			}
		}

		@Override
		public MockRestServiceServer build(RequestExpectationManager manager) {
			MockRestServiceServer server = new MockRestServiceServer(manager);
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
