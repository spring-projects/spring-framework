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
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.Retainable;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.*;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.*;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import static org.springframework.http.server.reactive.AbstractServerHttpRequest.QUERY_PATTERN;

/**
 * Adapt {@link HttpHandler} to the Jetty {@link org.eclipse.jetty.server.Handler} abstraction.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.1.4
 */
public class JettyCoreHttpHandlerAdapter extends Handler.Abstract.NonBlocking {

	private final HttpHandler httpHandler;
	private final DataBufferFactory dataBufferFactory;

	public JettyCoreHttpHandlerAdapter(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;

		// TODO currently we do not make a DataBufferFactory over the servers ByteBufferPool,
		//      because we mainly use wrap and there should be few allocation done by the factory.
		//      But it should be possible to use the servers buffer pool for allocations and to
		//      create PooledDataBuffers
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
			// then wrapped as a Flux.   The chunks are converted to RetainedDataBuffers with wrapping and can be
			// retained within a call to onNext.
			return Flux.from(FlowAdapters.toPublisher(Content.Source.asPublisher(request))).map(RetainedDataBuffer::new);
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

	class RetainedDataBuffer implements PooledDataBuffer
	{
		private final Retainable retainable;
		private final DataBuffer dataBuffer;
		private final AtomicBoolean allocated = new AtomicBoolean(true);

		public RetainedDataBuffer(Content.Chunk chunk) {
			this(chunk.getByteBuffer(), chunk);
		}

		public RetainedDataBuffer(ByteBuffer byteBuffer, Retainable retainable) {
			this.dataBuffer = dataBufferFactory.wrap(byteBuffer);
			this.retainable = retainable;
		}

		@Override
		public boolean isAllocated() {
			return allocated.get();
		}

		@Override
		public PooledDataBuffer retain() {
			retainable.retain();
			return this;
		}

		@Override
		public PooledDataBuffer touch(Object hint) {
			return this;
		}

		@Override
		public boolean release() {
			if (retainable.release()) {
				allocated.set(false);
				return true;
			}
			return false;
		}

		@Override
		public DataBufferFactory factory() {
			return dataBuffer.factory();
		}

		@Override
		public int indexOf(IntPredicate predicate, int fromIndex) {
			return dataBuffer.indexOf(predicate, fromIndex);
		}

		@Override
		public int lastIndexOf(IntPredicate predicate, int fromIndex) {
			return dataBuffer.lastIndexOf(predicate, fromIndex);
		}

		@Override
		public int readableByteCount() {
			return dataBuffer.readableByteCount();
		}

		@Override
		public int writableByteCount() {
			return dataBuffer.writableByteCount();
		}

		@Override
		public int capacity() {
			return dataBuffer.capacity();
		}

		@Override
		@Deprecated(since = "6.0")
		public DataBuffer capacity(int capacity) {
			return dataBuffer.capacity(capacity);
		}

		@Override
		@Deprecated(since = "6.0")
		public DataBuffer ensureCapacity(int capacity) {
			return dataBuffer.ensureCapacity(capacity);
		}

		@Override
		public DataBuffer ensureWritable(int capacity) {
			return dataBuffer.ensureWritable(capacity);
		}

		@Override
		public int readPosition() {
			return dataBuffer.readPosition();
		}

		@Override
		public DataBuffer readPosition(int readPosition) {
			return dataBuffer.readPosition(readPosition);
		}

		@Override
		public int writePosition() {
			return dataBuffer.writePosition();
		}

		@Override
		public DataBuffer writePosition(int writePosition) {
			return dataBuffer.writePosition(writePosition);
		}

		@Override
		public byte getByte(int index) {
			return dataBuffer.getByte(index);
		}

		@Override
		public byte read() {
			return dataBuffer.read();
		}

		@Override
		public DataBuffer read(byte[] destination) {
			return dataBuffer.read(destination);
		}

		@Override
		public DataBuffer read(byte[] destination, int offset, int length) {
			return dataBuffer.read(destination, offset, length);
		}

		@Override
		public DataBuffer write(byte b) {
			return dataBuffer.write(b);
		}

		@Override
		public DataBuffer write(byte[] source) {
			return dataBuffer.write(source);
		}

		@Override
		public DataBuffer write(byte[] source, int offset, int length) {
			return dataBuffer.write(source, offset, length);
		}

		@Override
		public DataBuffer write(DataBuffer... buffers) {
			return dataBuffer.write(buffers);
		}

		@Override
		public DataBuffer write(ByteBuffer... buffers) {
			return dataBuffer.write(buffers);
		}

		@Override
		public DataBuffer write(CharSequence charSequence, Charset charset) {
			return dataBuffer.write(charSequence, charset);
		}

		@Override
		@Deprecated(since = "6.0")
		public DataBuffer slice(int index, int length) {
			return dataBuffer.slice(index, length);
		}

		@Override
		@Deprecated(since = "6.0")
		public DataBuffer retainedSlice(int index, int length) {
			return dataBuffer.retainedSlice(index, length);
		}

		@Override
		public DataBuffer split(int index) {
			return dataBuffer.split(index);
		}

		@Override
		@Deprecated(since = "6.0")
		public ByteBuffer asByteBuffer() {
			return dataBuffer.asByteBuffer();
		}

		@Override
		@Deprecated(since = "6.0")
		public ByteBuffer asByteBuffer(int index, int length) {
			return dataBuffer.asByteBuffer(index, length);
		}

		@Override
		@Deprecated(since = "6.0.5")
		public ByteBuffer toByteBuffer() {
			return dataBuffer.toByteBuffer();
		}

		@Override
		@Deprecated(since = "6.0.5")
		public ByteBuffer toByteBuffer(int index, int length) {
			return dataBuffer.toByteBuffer(index, length);
		}

		@Override
		public void toByteBuffer(ByteBuffer dest) {
			dataBuffer.toByteBuffer(dest);
		}

		@Override
		public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
			dataBuffer.toByteBuffer(srcPos, dest, destPos, length);
		}

		@Override
		public ByteBufferIterator readableByteBuffers() {
			return dataBuffer.readableByteBuffers();
		}

		@Override
		public ByteBufferIterator writableByteBuffers() {
			return dataBuffer.writableByteBuffers();
		}

		@Override
		public InputStream asInputStream() {
			return dataBuffer.asInputStream();
		}

		@Override
		public InputStream asInputStream(boolean releaseOnClose) {
			return dataBuffer.asInputStream(releaseOnClose);
		}

		@Override
		public OutputStream asOutputStream() {
			return dataBuffer.asOutputStream();
		}

		@Override
		public String toString(Charset charset) {
			return dataBuffer.toString(charset);
		}

		@Override
		public String toString(int index, int length, Charset charset) {
			return dataBuffer.toString(index, length, charset);
		}
	}
}
