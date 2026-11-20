/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Simple implementation of {@link ClientHttpRequest} that wraps another request.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
final class BufferingClientHttpRequestWrapper extends AbstractBufferingClientHttpRequest {

	private final ClientHttpRequest request;


	BufferingClientHttpRequestWrapper(ClientHttpRequest request) {
		this.request = request;
	}


	@Override
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	@Override
	public URI getURI() {
		return this.request.getURI();
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		this.request.getHeaders().putAll(headers);
		return executeWithRequestAndBody(this.request, bufferedOutput, true);
	}

}
