/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Wraps another {@link ClientHttpRequest} and delegates all methods to it.
 * Subclasses can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ClientHttpRequestDecorator implements ClientHttpRequest {

	private final ClientHttpRequest delegate;


	public ClientHttpRequestDecorator(ClientHttpRequest delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}


	public ClientHttpRequest getDelegate() {
		return this.delegate;
	}


	// ClientHttpRequest delegation methods...

	@Override
	public HttpMethod getMethod() {
		return this.delegate.getMethod();
	}

	@Override
	public URI getURI() {
		return this.delegate.getURI();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.delegate.getHeaders();
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		return this.delegate.getCookies();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.delegate.getAttributes();
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.delegate.bufferFactory();
	}

	@Override
	public <T> T getNativeRequest() {
		return this.delegate.getNativeRequest();
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		this.delegate.beforeCommit(action);
	}

	@Override
	public boolean isCommitted() {
		return this.delegate.isCommitted();
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return this.delegate.writeWith(body);
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return this.delegate.writeAndFlushWith(body);
	}

	@Override
	public Mono<Void> setComplete() {
		return this.delegate.setComplete();
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
	}

}
