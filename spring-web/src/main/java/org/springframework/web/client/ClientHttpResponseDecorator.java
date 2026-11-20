/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;


/**
 * Wrap and delegate to an existing {@link ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
class ClientHttpResponseDecorator implements ClientHttpResponse {

	private final ClientHttpResponse delegate;


	public ClientHttpResponseDecorator(ClientHttpResponse delegate) {
		Assert.notNull(delegate, "ClientHttpResponse delegate is required");
		this.delegate = delegate;
	}


	/**
	 * Return the wrapped response.
	 */
	public ClientHttpResponse getDelegate() {
		return this.delegate;
	}


	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return this.delegate.getStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.delegate.getStatusText();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.delegate.getHeaders();
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.delegate.getBody();
	}

	@Override
	public void close() {
		this.delegate.close();
	}

}
