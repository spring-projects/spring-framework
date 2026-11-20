/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.Assert;

/**
 * Implementation of {@code ServerHttpResponse} that delegates all calls to a
 * given target {@code ServerHttpResponse}.
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
public class DelegatingServerHttpResponse implements ServerHttpResponse {

	private final ServerHttpResponse delegate;

	/**
	 * Create a new {@code DelegatingServerHttpResponse}.
	 * @param delegate the response to delegate to
	 */
	public DelegatingServerHttpResponse(ServerHttpResponse delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Returns the target response that this response delegates to.
	 * @return the delegate
	 */
	public ServerHttpResponse getDelegate() {
		return this.delegate;
	}

	@Override
	public void setStatusCode(HttpStatusCode status) {
		this.delegate.setStatusCode(status);
	}

	@Override
	public void flush() throws IOException {
		this.delegate.flush();
	}

	@Override
	public void close() {
		this.delegate.close();
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.delegate.getBody();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.delegate.getHeaders();
	}

}
