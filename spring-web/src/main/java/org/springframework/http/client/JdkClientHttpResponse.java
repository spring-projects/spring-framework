/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation based on the Java {@link HttpClient}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @since 6.1
 */
class JdkClientHttpResponse implements ClientHttpResponse {

	private final HttpResponse<InputStream> response;

	private final HttpHeaders headers;

	private final InputStream body;


	JdkClientHttpResponse(HttpResponse<InputStream> response, Consumer<HttpHeaders> headersConsumer, @Nullable InputStream body) {
		this.response = response;
		this.headers = adaptHeaders(response, headersConsumer);
		this.body = (body != null ? body : InputStream.nullInputStream());
	}

	private static HttpHeaders adaptHeaders(HttpResponse<?> response, Consumer<HttpHeaders> headersConsumer) {
		Map<String, List<String>> rawHeaders = response.headers().map();
		HttpHeaders headers = new HttpHeaders();
		rawHeaders.forEach(headers::put);
		headersConsumer.accept(headers);
		return HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatusCode.valueOf(this.response.statusCode());
	}

	@Override
	public String getStatusText() {
		// HttpResponse does not expose status text
		if (getStatusCode() instanceof HttpStatus status) {
			return status.getReasonPhrase();
		}
		else {
			return "";
		}
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
			try {
				StreamUtils.drain(this.body);
			}
			finally {
				this.body.close();
			}
		}
		catch (IOException ignored) {
		}
	}

}
