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
import org.eclipse.jetty.util.Callback;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.server.RequestPath;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 *
 * @author Greg Wilkins
 * @since 6.0
 */
public class JettyCoreHttpHandlerAdapter extends Handler.Abstract {

	private final HttpHandler httpHandler;

	public JettyCoreHttpHandlerAdapter(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;
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

	private static class JettyCoreServerHttpRequest implements ServerHttpRequest {
		private final Request request;
		private final HttpHeaders headers;
		private final RequestPath path;
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
			return request.getHttpURI().toURI();
		}

		@Override
		public Flux<DataBuffer> getBody() {
			Flow.Publisher<Content.Chunk> flowPublisher = Content.Source.asPublisher(request);
			// TODO convert the Flow.Publisher into a org.reactivestreams.Publisher
			org.reactivestreams.Publisher publisher = null;
			// TODO convert the Publisher to a Flux
			Flux<Content.Chunk> chunks = Flux.from(publisher);
			// TODO map the chunks to DataBuffers
			return chunks.map(chunk -> null);
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
			return null;
		}

		@Override
		public MultiValueMap<String, HttpCookie> getCookies() {
			if (cookies == null) {
				LinkedHashMap<String, List<HttpCookie>> map = new LinkedHashMap<>();
				for (org.eclipse.jetty.http.HttpCookie c : Request.getCookies(request)) {
					List<HttpCookie> list = map.computeIfAbsent(c.getName(), k -> new ArrayList<>());
					list.add(new HttpCookie(c.getName(), c.getValue()));
				}
				cookies = new LinkedMultiValueMap<>(map);
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
			request.addHttpStreamWrapper(s -> new HttpStream.Wrapper(s)
			{
				@Override
				public void send(MetaData.Request metaDataRequest, @Nullable MetaData.Response metaDataResponse, boolean last, ByteBuffer content, Callback callback) {

					if (metaDataResponse != null)
						request.getContext().run(JettyCoreServerHttpResponse.this::onCommit, request);
					if (last)
						callback = Callback.from(callback, JettyCoreServerHttpResponse.this::onLast);

					super.send(metaDataRequest, metaDataResponse, last, content, callback);
				}

				@Override
				public void succeeded() {
					super.succeeded();
					onCompleted(null);
				}

				@Override
				public void failed(Throwable x) {
					super.failed(x);
					onCompleted(x);
				}
			});
		}

		private void onCommit() {
			// TODO call all the beforeCommit actions
		}

		private void onLast() {

		}

		private void onCompleted(@Nullable Throwable failure) {
			// TODO trigger any setComplete Monos
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
			// TODO
		}

		@Override
		public boolean isCommitted() {
			return response.isCommitted();
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			// TODO
			return null;
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			// TODO
			return null;
		}

		@Override
		public Mono<Void> setComplete() {
			// TODO
			return null;
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
