/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.client.response;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * {@code ResponseCreator} that obtains the response by executing the request
 * through a {@link ClientHttpRequestFactory}. This is useful in scenarios with
 * multiple remote services where some need to be called rather than mocked.
 * <p>The {@code ClientHttpRequestFactory} is typically obtained from the
 * {@code RestTemplate} before it is passed to {@code MockRestServiceServer},
 * in effect using the original factory rather than the test factory:
 * <pre><code>
 * ResponseCreator withActualResponse = new ExecutingResponseCreator(restTemplate);
 * MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
 * //...
 * server.expect(requestTo("/foo")).andRespond(withSuccess());
 * server.expect(requestTo("/bar")).andRespond(withActualResponse);
 * </code></pre>
 *
 * @author Simon Baslé
 * @since 6.0.4
 */
public class ExecutingResponseCreator implements ResponseCreator {

	private final ClientHttpRequestFactory requestFactory;


	/**
	 * Create an instance with the given {@code ClientHttpRequestFactory}.
	 * @param requestFactory the request factory to delegate to
	 */
	public ExecutingResponseCreator(ClientHttpRequestFactory requestFactory) {
		this.requestFactory = requestFactory;
	}


	@Override
	public ClientHttpResponse createResponse(@Nullable ClientHttpRequest request) throws IOException {
		Assert.state(request instanceof MockClientHttpRequest, "Expected a MockClientHttpRequest");
		MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
		ClientHttpRequest newRequest = this.requestFactory.createRequest(mockRequest.getURI(), mockRequest.getMethod());
		newRequest.getHeaders().putAll(mockRequest.getHeaders());
		StreamUtils.copy(mockRequest.getBodyAsBytes(), newRequest.getBody());
		return newRequest.execute();
	}
}
