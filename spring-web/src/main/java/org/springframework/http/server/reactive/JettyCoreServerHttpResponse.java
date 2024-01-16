/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.server.HttpCookieUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Adapt an Eclipse Jetty {@link Response} to a {@link org.springframework.http.server.ServerHttpResponse}
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.1.4
 */
class JettyCoreServerHttpResponse implements ServerHttpResponse {
	enum State {
		OPEN, COMMITTED, LAST, COMPLETED
	}

	private final AtomicReference<State> state = new AtomicReference<>(State.OPEN);
	private final Request request;
	private final Response response;
	private final HttpHeaders headers;

	public JettyCoreServerHttpResponse(Request request, Response response) {
		this.request = request;
		this.response = response;
		headers = new HttpHeaders(new JettyHeadersAdapter(response.getHeaders()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return headers;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		// TODO
		return null;
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		// TODO See UndertowServerHttpResponse as an example
	}

	@Override
	public boolean isCommitted() {
		return response.isCommitted();
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// TODO
		return Mono.empty();
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		// TODO
		return Mono.empty();
	}

	@Override
	public Mono<Void> setComplete() {
		// TODO
		return Mono.empty();
	}

	@Override
	public boolean setStatusCode(@Nullable HttpStatusCode status) {
		if (isCommitted() || status == null)
			return false;
		response.setStatus(status.value());
		return true;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatusCode.valueOf(response.getStatus());
	}

	@Override
	public boolean setRawStatusCode(@Nullable Integer value) {
		if (isCommitted() || value == null)
			return false;
		response.setStatus(value);
		return true;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		LinkedMultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
		for (HttpField f : response.getHeaders()) {
			if (f instanceof HttpCookieUtils.SetCookieHttpField setCookieHttpField && setCookieHttpField.getHttpCookie() instanceof HttpResponseCookie httpResponseCookie)
				cookies.add(httpResponseCookie.getName(), httpResponseCookie.getResponseCookie());
		}
		return cookies;
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		Response.addCookie(response, new HttpResponseCookie(cookie));
	}

	private class HttpResponseCookie implements org.eclipse.jetty.http.HttpCookie {
		private final ResponseCookie responseCookie;

		public HttpResponseCookie(ResponseCookie responseCookie) {
			this.responseCookie = responseCookie;
		}

		public ResponseCookie getResponseCookie() {
			return responseCookie;
		}

		@Override
		public String getName() {
			return responseCookie.getName();
		}

		@Override
		public String getValue() {
			return responseCookie.getValue();
		}

		@Override
		public int getVersion() {
			return 0;
		}

		@Override
		public long getMaxAge() {
			return responseCookie.getMaxAge().toSeconds();
		}

		@Override
		@Nullable
		public String getComment() {
			return null;
		}

		@Override
		@Nullable
		public String getDomain() {
			return responseCookie.getDomain();
		}

		@Override
		@Nullable
		public String getPath() {
			return responseCookie.getPath();
		}

		@Override
		public boolean isSecure() {
			return responseCookie.isSecure();
		}

		@Override
		public SameSite getSameSite() {
			String sameSiteName = responseCookie.getSameSite();
			if (sameSiteName != null)
				return SameSite.valueOf(sameSiteName);
			SameSite sameSite = HttpCookieUtils.getSameSiteDefault(request.getContext());
			return sameSite == null ? SameSite.NONE : sameSite;
		}

		@Override
		public boolean isHttpOnly() {
			return responseCookie.isHttpOnly();
		}

		@Override
		public boolean isPartitioned() {
			return false;
		}

		@Override
		public Map<String, String> getAttributes() {
			return Collections.emptyMap();
		}
	}
}
