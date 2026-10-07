/*
 * Copyright 2002-present the original author or authors.
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequest} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 6.0
 */
class JdkClientHttpRequest extends AbstractClientHttpRequest {

	private static final Set<String> DISALLOWED_HEADERS = disallowedHeaders();


	private final HttpMethod method;

	private final URI uri;

	private final DataBufferFactory bufferFactory;

	private final HttpRequest.Builder builder;


	public JdkClientHttpRequest(
			HttpMethod httpMethod, URI uri, DataBufferFactory bufferFactory, @Nullable Duration readTimeout) {

		Assert.notNull(httpMethod, "HttpMethod is required");
		Assert.notNull(uri, "URI is required");
		Assert.notNull(bufferFactory, "DataBufferFactory is required");

		this.method = httpMethod;
		this.uri = uri;
		this.bufferFactory = bufferFactory;
		this.builder = HttpRequest.newBuilder(uri);
		if (readTimeout != null) {
			this.builder.timeout(readTimeout);
		}
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this.builder.build();
	}


	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> {
			this.builder.method(this.method.name(), toBodyPublisher(body));
			return Mono.empty();
		});
	}

	private HttpRequest.BodyPublisher toBodyPublisher(Publisher<? extends DataBuffer> body) {
		Publisher<ByteBuffer> byteBufferBody = (body instanceof Mono ?
				Mono.from(body).map(this::toByteBuffer) :
				Flux.from(body).map(this::toByteBuffer));

		Flow.Publisher<ByteBuffer> bodyFlow = JdkFlowAdapter.publisherToFlowPublisher(byteBufferBody);

		return (getHeaders().getContentLength() > 0 ?
				HttpRequest.BodyPublishers.fromPublisher(bodyFlow, getHeaders().getContentLength()) :
				HttpRequest.BodyPublishers.fromPublisher(bodyFlow));
	}

	private ByteBuffer toByteBuffer(DataBuffer dataBuffer) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
		dataBuffer.toByteBuffer(byteBuffer);
		return byteBuffer;
	}

	@Override
	public Mono<Void> writeAndFlushWith(final Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body).flatMap(Function.identity()));
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(() -> {
			this.builder.method(this.method.name(), HttpRequest.BodyPublishers.noBody());
			return Mono.empty();
		});
	}

	@Override
	protected void applyHeaders() {
		for (Map.Entry<String, List<String>> entry : getHeaders().headerSet()) {
			if (entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
				// content-length is specified when writing
				continue;
			}
			if (DISALLOWED_HEADERS.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
				continue;
			}
			for (String value : entry.getValue()) {
				this.builder.header(entry.getKey(), value);
			}
		}
		if (!getHeaders().containsHeader(HttpHeaders.ACCEPT)) {
			this.builder.header(HttpHeaders.ACCEPT, "*/*");
		}
	}

	@Override
	protected void applyCookies() {
		MultiValueMap<String, HttpCookie> cookies = getCookies();
		if (cookies.isEmpty()) {
			return;
		}
		this.builder.header(HttpHeaders.COOKIE, cookies.values().stream()
				.flatMap(List::stream).map(HttpCookie::toString).collect(Collectors.joining(";")));
	}

	/**
	 * By default, {@link HttpRequest} does not allow {@code Connection},
	 * {@code Content-Length}, {@code Expect}, {@code Host}, or {@code Upgrade}
	 * headers to be set, but this can be overridden with the
	 * {@code jdk.httpclient.allowRestrictedHeaders} system property.
	 * <p>Note that only the system property is consulted. In contrast to the
	 * JDK, a value configured in {@code $JAVA_HOME/conf/net.properties} is
	 * not taken into account.
	 * @see jdk.internal.net.http.common.Utils#getDisallowedHeaders()
	 */
	static Set<String> disallowedHeaders() {
		TreeSet<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		headers.addAll(Set.of("connection", "content-length", "expect", "host", "upgrade"));

		String headersToAllow = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
		if (headersToAllow != null) {
			Set<String> toAllow = StringUtils.commaDelimitedListToSet(headersToAllow);
			headers.removeAll(toAllow);
		}
		return Collections.unmodifiableSet(headers);
	}

}
