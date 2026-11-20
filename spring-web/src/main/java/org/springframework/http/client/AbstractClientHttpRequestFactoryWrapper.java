/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link ClientHttpRequestFactory} implementations
 * that decorate another delegate request factory.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public abstract class AbstractClientHttpRequestFactoryWrapper implements ClientHttpRequestFactory {

	private final ClientHttpRequestFactory requestFactory;


	/**
	 * Create a {@code AbstractClientHttpRequestFactoryWrapper} wrapping the given request factory.
	 * @param requestFactory the request factory to be wrapped
	 */
	protected AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
		this.requestFactory = requestFactory;
	}


	/**
	 * This implementation simply calls {@link #createRequest(URI, HttpMethod, ClientHttpRequestFactory)}
	 * with the wrapped request factory provided to the
	 * {@linkplain #AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory) constructor}.
	 */
	@Override
	public final ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequest(uri, httpMethod, this.requestFactory);
	}

	/**
	 * Return the delegate request factory.
	 * @since 6.1.4
	 */
	public ClientHttpRequestFactory getDelegate() {
		return this.requestFactory;
	}

	/**
	 * Create a new {@link ClientHttpRequest} for the specified URI and HTTP method
	 * by using the passed-on request factory.
	 * <p>Called from {@link #createRequest(URI, HttpMethod)}.
	 * @param uri the URI to create a request for
	 * @param httpMethod the HTTP method to execute
	 * @param requestFactory the wrapped request factory
	 * @return the created request
	 * @throws IOException in case of I/O errors
	 */
	protected abstract ClientHttpRequest createRequest(
			URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException;

}
