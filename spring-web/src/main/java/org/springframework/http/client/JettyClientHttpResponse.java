/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jetty.client.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation based on Jetty's
 * {@link org.eclipse.jetty.client.HttpClient}.
 *
 * @author Arjen Poutsma
 * @since 6.1
 */
class JettyClientHttpResponse implements ClientHttpResponse {

	private final Response response;

	private final InputStream body;

	private final HttpHeaders headers;


	public JettyClientHttpResponse(Response response, InputStream inputStream) {
		this.response = response;
		this.body = inputStream;

		MultiValueMap<String, String> headers = new JettyHeadersAdapter(response.getHeaders());
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return HttpStatusCode.valueOf(this.response.getStatus());
	}

	@Override
	public String getStatusText() throws IOException {
		return this.response.getReason();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	@Override
	public void close() {
		try {
			this.body.close();
		}
		catch (IOException ignored) {
		}
	}
}
