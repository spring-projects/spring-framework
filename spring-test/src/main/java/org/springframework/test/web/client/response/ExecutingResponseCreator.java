/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client.response;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * {@code ResponseCreator} that obtains the response by executing the request
 * through a {@link ClientHttpRequestFactory}. This is useful in scenarios with
 * multiple remote services where some need to be called rather than mocked.
 * <p>The {@code ClientHttpRequestFactory} is typically used for building the
 * {@code RestClient} and is passed to {@code MockRestServiceServer},
 * in effect using the original factory rather than the test factory:
 * <pre><code>
 * RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
 * ResponseCreator withActualResponse = new ExecutingResponseCreator(requestFactory);
 * MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
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
