/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.reactive;

import org.eclipse.jetty.reactive.client.ReactiveResponse;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">
 *     Jetty ReactiveStreams HttpClient</a>
 */
class JettyClientHttpResponse extends AbstractClientHttpResponse {

	public JettyClientHttpResponse(
			ReactiveResponse response, Flux<DataBuffer> content,
			MultiValueMap<String, ResponseCookie> cookies) {

		super(HttpStatusCode.valueOf(response.getStatus()), adaptHeaders(response), cookies, content);
	}

	private static HttpHeaders adaptHeaders(ReactiveResponse response) {
		MultiValueMap<String, String> headers = new JettyHeadersAdapter(response.getHeaders());
		return HttpHeaders.readOnlyHttpHeaders(headers);
	}

}
