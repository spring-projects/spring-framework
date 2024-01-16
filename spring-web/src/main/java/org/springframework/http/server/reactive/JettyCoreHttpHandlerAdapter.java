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
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.*;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.server.RequestPath;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import static org.springframework.http.server.reactive.AbstractServerHttpRequest.QUERY_PATTERN;

/**
 *
 * Adapt {@link HttpHandler} to the Jetty {@link org.eclipse.jetty.server.Handler} abstraction.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.1.4
 */
public class JettyCoreHttpHandlerAdapter extends Handler.Abstract {

	private final HttpHandler httpHandler;
	private final DataBufferFactory dataBufferFactory;

	public JettyCoreHttpHandlerAdapter(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;

		// We do not make a DataBufferFactory over the servers ByteBufferPool, because we only ever use
		// wrap and there should rarely be any allocation done by the factory.  Also, there is no release semantic
		// available so we could not do retainable buffers anyway.
		dataBufferFactory = new DefaultDataBufferFactory();
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		httpHandler.handle(new JettyCoreServerHttpRequest(request), new JettyCoreServerHttpResponse(request, response))
				.subscribe(new Subscriber<>() {
					@Override
					public void onSubscribe(Subscription s) {
						s.request(Long.MAX_VALUE);
					}

					@Override
					public void onNext(Void unused) {
						// we can ignore the void as we only seek onError or onComplete
					}

					@Override
					public void onError(Throwable t) {
						callback.failed(t);
					}

					@Override
					public void onComplete() {
						callback.succeeded();
					}
				});
		return true;
	}

	private class JettyCoreServerHttpRequest implements ServerHttpRequest {
		private final static MultiValueMap<String, String> EMPTY_QUERY = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());
		private final static MultiValueMap<String, HttpCookie> EMPTY_COOKIES = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());
		private final Request request;
		private final HttpHeaders headers;
		private final RequestPath path;
		@Nullable
		private URI uri;
		@Nullable
		MultiValueMap<String, String> queryParameters;
		@Nullable
		private MultiValueMap<String, HttpCookie> cookies;

		public JettyCoreServerHttpRequest(Request request) {
			this.request = request;
			headers = new HttpHeaders(new JettyHeadersAdapter(request.getHeaders()));
			path = RequestPath.parse(request.getHttpURI().getCanonicalPath(), request.getContext().getContextPath());
		}

		@Override
		public HttpHeaders getHeaders() {
			return headers;
		}

		@Override
		public HttpMethod getMethod() {
			return HttpMethod.valueOf(request.getMethod());
		}

		@Override
		public URI getURI() {
			if (uri == null)
				uri = request.getHttpURI().toURI();
			return uri;
		}

		@Override
		public Flux<DataBuffer> getBody() {
			// We access the request body as a Flow.Publisher, which is wrapped as an org.reactivestreams.Publisher and
			// then wrapped as a Flux.   The chunks are converted to DataBuffers with simple wrapping and will be released
			// by the Flow.Publisher on return from onNext, so that any retention of the data must be done by a copy within
			// the call to onNext.
			return Flux.from(FlowAdapters.toPublisher(Content.Source.asPublisher(request))).map(chunk -> dataBufferFactory.wrap(chunk.getByteBuffer()));
		}

		@Override
		public String getId() {
			return request.getId();
		}

		@Override
		public RequestPath getPath() {
			return path;
		}

		@Override
		public MultiValueMap<String, String> getQueryParams() {
			if (queryParameters == null)
			{
				String query = request.getHttpURI().getQuery();
				if (StringUtil.isBlank(query))
					queryParameters = EMPTY_QUERY;
				else {
					queryParameters = new LinkedMultiValueMap<>();
					Matcher matcher = QUERY_PATTERN.matcher(query);
					while (matcher.find()) {
						String name = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
						String eq = matcher.group(2);
						String value = matcher.group(3);
						value = (value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : (StringUtils.hasLength(eq) ? "" : null));
						queryParameters.add(name, value);
					}
				}
			}
			return queryParameters;
		}

		@Override
		public MultiValueMap<String, HttpCookie> getCookies() {
			if (cookies == null) {
				List<org.eclipse.jetty.http.HttpCookie> httpCookies = Request.getCookies(request);
				if (httpCookies.isEmpty())
					cookies = EMPTY_COOKIES;
				else {
					cookies = new LinkedMultiValueMap<>();
					for (org.eclipse.jetty.http.HttpCookie c : httpCookies) {
						cookies.add(c.getName(), new HttpCookie(c.getName(), c.getValue()));
					}
					cookies = CollectionUtils.unmodifiableMultiValueMap(cookies);
				}
			}
			return cookies;
		}

		@Override
		public InetSocketAddress getLocalAddress() {
			return request.getConnectionMetaData().getLocalSocketAddress() instanceof InetSocketAddress inet
					? inet : null;
		}

		@Override
		public InetSocketAddress getRemoteAddress() {
			return request.getConnectionMetaData().getRemoteSocketAddress() instanceof InetSocketAddress inet
					? inet : null;
		}

		@Override
		public SslInfo getSslInfo() {
			if (request.getConnectionMetaData().isSecure() && request.getAttribute(EndPoint.SslSessionData.ATTRIBUTE) instanceof EndPoint.SslSessionData sslSessionData) {
				return new SslInfo()
				{
					@Override
					public String getSessionId() {
						return sslSessionData.sslSessionId();
					}

					@Override
					public X509Certificate[] getPeerCertificates() {
						return sslSessionData.peerCertificates();
					}
				};
			}
			return null;
		}

		@Override
		public Builder mutate() {
			return ServerHttpRequest.super.mutate();
		}
	}

	private static class JettyCoreServerHttpResponse implements ServerHttpResponse {
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
}
