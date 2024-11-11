/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import io.netty5.handler.codec.http.headers.DefaultHttpSetCookie;
import io.netty5.handler.codec.http.headers.HttpSetCookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.netty5.ChannelOperationsId;
import reactor.netty5.Connection;
import reactor.netty5.NettyInbound;
import reactor.netty5.http.client.HttpClientResponse;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.support.Netty5HeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * {@link ClientHttpResponse} implementation for the Reactor Netty 2 (Netty 5) HTTP client.
 *
 * <p>This class is based on {@link ReactorClientHttpResponse}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 * @see reactor.netty5.http.client.HttpClient
 */
class ReactorNetty2ClientHttpResponse implements ClientHttpResponse {

	private static final Log logger = LogFactory.getLog(ReactorNetty2ClientHttpResponse.class);

	private final HttpClientResponse response;

	private final HttpHeaders headers;

	private final NettyInbound inbound;

	private final Netty5DataBufferFactory bufferFactory;

	// 0 - not subscribed, 1 - subscribed, 2 - cancelled via connector (before subscribe)
	private final AtomicInteger state = new AtomicInteger();


	/**
	 * Constructor that matches the inputs from
	 * {@link reactor.netty5.http.client.HttpClient.ResponseReceiver#responseConnection(BiFunction)}.
	 * @since 5.2.8
	 */
	public ReactorNetty2ClientHttpResponse(HttpClientResponse response, Connection connection) {
		this.response = response;
		MultiValueMap<String, String> adapter = new Netty5HeadersAdapter(response.responseHeaders());
		this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
		this.inbound = connection.inbound();
		this.bufferFactory = new Netty5DataBufferFactory(connection.outbound().alloc());
	}


	@Override
	public String getId() {
		String id = null;
		if (this.response instanceof ChannelOperationsId operationsId) {
			id = (logger.isDebugEnabled() ? operationsId.asLongText() : operationsId.asShortText());
		}
		if (id == null && this.response instanceof Connection connection) {
			id = connection.channel().id().asShortText();
		}
		return (id != null ? id : ObjectUtils.getIdentityHexString(this));
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.inbound.receive()
				.doOnSubscribe(s -> {
					if (this.state.compareAndSet(0, 1)) {
						return;
					}
					if (this.state.get() == 2) {
						throw new IllegalStateException(
								"The client response body has been released already due to cancellation.");
					}
				})
				.map(buffer -> this.bufferFactory.wrap(buffer.split()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatusCode.valueOf(this.response.status().code());
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		this.response.cookies().values().stream()
				.flatMap(Collection::stream)
				.forEach(cookie -> result.add(cookie.name().toString(),
						ResponseCookie.fromClientResponse(cookie.name().toString(), cookie.value().toString())
								.domain(toString(cookie.domain()))
								.path(toString(cookie.path()))
								.maxAge(toLong(cookie.maxAge()))
								.secure(cookie.isSecure())
								.httpOnly(cookie.isHttpOnly())
								.sameSite(getSameSite(cookie))
								.build()));
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Nullable
	private static String toString(@Nullable CharSequence value) {
		return (value != null ? value.toString() : null);
	}

	private static long toLong(@Nullable Long value) {
		return (value != null ? value : -1);
	}

	@Nullable
	private static String getSameSite(HttpSetCookie cookie) {
		if (cookie instanceof DefaultHttpSetCookie defaultCookie && defaultCookie.sameSite() != null) {
			return defaultCookie.sameSite().name();
		}
		return null;
	}

	/**
	 * Called by {@link ReactorNetty2ClientHttpConnector} when a cancellation is detected
	 * but the content has not been subscribed to. If the subscription never
	 * materializes then the content will remain not drained. Or it could still
	 * materialize if the cancellation happened very early, or the response
	 * reading was delayed for some reason.
	 */
	void releaseAfterCancel(HttpMethod method) {
		if (mayHaveBody(method) && this.state.compareAndSet(0, 2)) {
			if (logger.isDebugEnabled()) {
				logger.debug("[" + getId() + "]" + "Releasing body, not yet subscribed.");
			}
			this.inbound.receive().doOnNext(buffer -> {}).subscribe(buffer -> {}, ex -> {});
		}
	}

	private boolean mayHaveBody(HttpMethod method) {
		int code = getStatusCode().value();
		return !((code >= 100 && code < 200) || code == 204 || code == 205 ||
				method.equals(HttpMethod.HEAD) || getHeaders().getContentLength() == 0);
	}

	@Override
	public String toString() {
		return "ReactorNetty2ClientHttpResponse{" +
				"request=[" + this.response.method().name() + " " + this.response.uri() + "]," +
				"status=" + getStatusCode() + '}';
	}

}
