/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.function.BiPredicate;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequestFactory} that wraps another in order to buffer
 * outgoing and incoming content in memory, making it possible to set a
 * content-length on the request, and to read the
 * {@linkplain ClientHttpResponse#getBody() response body} multiple times.
 *
 * <p><strong>Note:</strong> as of 7.0, buffering can be enabled through
 * {@link org.springframework.web.client.RestClient.Builder#bufferContent(BiPredicate)}
 * and therefore it is not necessary for applications to use this class directly.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class BufferingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	private final BiPredicate<URI, HttpMethod> bufferingPredicate;


	/**
	 * Create a buffering wrapper for the given {@link ClientHttpRequestFactory}.
	 * @param requestFactory the target request factory to wrap
	 */
	public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
		this(requestFactory, null);
	}

	/**
	 * Constructor variant with an additional predicate to decide whether to
	 * buffer the response.
	 * @since 7.0
	 */
	public BufferingClientHttpRequestFactory(
			ClientHttpRequestFactory requestFactory,
			@Nullable BiPredicate<URI, HttpMethod> bufferingPredicate) {

		super(requestFactory);
		this.bufferingPredicate = (bufferingPredicate != null ? bufferingPredicate : (uri, method) -> true);
	}



	@Override
	protected ClientHttpRequest createRequest(
			URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException {

		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		return (shouldBuffer(uri, httpMethod) ? new BufferingClientHttpRequestWrapper(request) : request);
	}

	/**
	 * Indicates whether the request/response exchange for the given URI and method
	 * should be buffered in memory.
	 * <p>The default implementation returns {@code true} for all URIs and methods.
	 * Subclasses can override this method to change this behavior.
	 * @param uri the URI
	 * @param httpMethod the method
	 * @return {@code true} if the exchange should be buffered; {@code false} otherwise
	 */
	protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
		return this.bufferingPredicate.test(uri, httpMethod);
	}

}
