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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.RetainableByteBuffer;
import org.eclipse.jetty.server.HttpCookieUtils;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.IteratingCallback;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Adapt an Eclipse Jetty {@link Response} to a {@link org.springframework.http.server.ServerHttpResponse}.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.2
 */
class JettyCoreServerHttpResponse implements ServerHttpResponse, ZeroCopyHttpOutputMessage {
	private final AtomicBoolean committed = new AtomicBoolean(false);

	private final List<Supplier<? extends Mono<Void>>> commitActions = new CopyOnWriteArrayList<>();

	private final Response response;

	private final HttpHeaders headers;

	@Nullable
	private LinkedMultiValueMap<String, ResponseCookie> cookies;

	public JettyCoreServerHttpResponse(Response response) {
		this.response = response;
		this.headers = new HttpHeaders(new JettyHeadersAdapter(response.getHeaders()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return DefaultDataBufferFactory.sharedInstance;
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		this.commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		return this.committed.get();
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return Flux.from(body)
				.flatMap(this::sendDataBuffer, 1)
				.then();
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return Flux.from(body)
				.flatMap(this::writeWith, 1)
				.then();
	}

	@Override
	public Mono<Void> setComplete() {
		Mono<Void> mono = ensureCommitted();
		return (mono == null) ? Mono.empty() : mono;
	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count) {
		Mono<Void> mono = ensureCommitted();
		if (mono != null) {
			return mono.then(Mono.defer(() -> writeWith(file, position, count)));
		}

		Callback.Completable callback = new Callback.Completable();
		mono = Mono.fromFuture(callback);
		try {
			// The method can block, but it is not expected to do so for any significant time.
			@SuppressWarnings("BlockingMethodInNonBlockingContext")
			SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
			new ContentWriterIteratingCallback(channel, position, count, this.response, callback).iterate();
		}
		catch (Throwable th) {
			callback.failed(th);
		}
		return mono;
	}

	@Nullable
	private Mono<Void> ensureCommitted() {
		if (this.committed.compareAndSet(false, true)) {
			if (!this.commitActions.isEmpty()) {
				return Flux.concat(Flux.fromIterable(this.commitActions).map(Supplier::get))
						.concatWith(Mono.fromRunnable(this::writeCookies))
						.then()
						.doOnError(t -> getHeaders().clearContentHeaders());
			}

			writeCookies();
		}

		return null;
	}

	private void writeCookies() {
		if (this.cookies != null) {
			this.cookies.values().stream()
					.flatMap(List::stream)
					.forEach(cookie -> Response.addCookie(this.response, new HttpResponseCookie(cookie)));
		}
	}

	private Mono<Void> sendDataBuffer(DataBuffer dataBuffer) {
		Mono<Void> mono = ensureCommitted();
		if (mono != null) {
			return mono.then(Mono.defer(() -> sendDataBuffer(dataBuffer)));
		}

		@SuppressWarnings("resource")
		DataBuffer.ByteBufferIterator byteBufferIterator = dataBuffer.readableByteBuffers();
		Callback.Completable callback = new Callback.Completable();
		new IteratingCallback() {
			@Override
			protected Action process() {
				if (!byteBufferIterator.hasNext()) {
					return Action.SUCCEEDED;
				}
				response.write(false, byteBufferIterator.next(), this);
				return Action.SCHEDULED;
			}

			@Override
			protected void onCompleteSuccess() {
				byteBufferIterator.close();
				DataBufferUtils.release(dataBuffer);
				callback.complete(null);
			}

			@Override
			protected void onCompleteFailure(Throwable cause) {
				byteBufferIterator.close();
				DataBufferUtils.release(dataBuffer);
				callback.failed(cause);
			}
		}.iterate();

		return Mono.fromFuture(callback);
	}

	@Override
	public boolean setStatusCode(@Nullable HttpStatusCode status) {
		if (isCommitted() || status == null) {
			return false;
		}
		this.response.setStatus(status.value());
		return true;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		int status = this.response.getStatus();
		return HttpStatusCode.valueOf(status == 0 ? 200 : status);
	}

	@Override
	public boolean setRawStatusCode(@Nullable Integer value) {
		if (isCommitted() || value == null) {
			return false;
		}
		this.response.setStatus(value);
		return true;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return initializeCookies();
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		initializeCookies().add(cookie.getName(), cookie);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.response;
	}

	private LinkedMultiValueMap<String, ResponseCookie> initializeCookies() {
		if (this.cookies == null) {
			this.cookies = new LinkedMultiValueMap<>();
			// remove all existing cookies from the response and add them to the cookie map, to be added back later
			for (ListIterator<HttpField> i = this.response.getHeaders().listIterator(); i.hasNext(); ) {
				HttpField f = i.next();
				if (f instanceof HttpCookieUtils.SetCookieHttpField setCookieHttpField) {
					HttpCookie httpCookie = setCookieHttpField.getHttpCookie();
					ResponseCookie responseCookie = ResponseCookie.from(httpCookie.getName(), httpCookie.getValue())
							.httpOnly(httpCookie.isHttpOnly())
							.domain(httpCookie.getDomain())
							.maxAge(httpCookie.getMaxAge())
							.sameSite(httpCookie.getSameSite().name())
							.secure(httpCookie.isSecure())
							.build();
					this.cookies.add(responseCookie.getName(), responseCookie);
					i.remove();
				}
			}
		}
		return this.cookies;
	}

	private static class HttpResponseCookie implements org.eclipse.jetty.http.HttpCookie {
		private final ResponseCookie responseCookie;

		public HttpResponseCookie(ResponseCookie responseCookie) {
			this.responseCookie = responseCookie;
		}

		public ResponseCookie getResponseCookie() {
			return this.responseCookie;
		}

		@Override
		public String getName() {
			return this.responseCookie.getName();
		}

		@Override
		public String getValue() {
			return this.responseCookie.getValue();
		}

		@Override
		public int getVersion() {
			return 0;
		}

		@Override
		public long getMaxAge() {
			return this.responseCookie.getMaxAge().toSeconds();
		}

		@Override
		@Nullable
		public String getComment() {
			return null;
		}

		@Override
		@Nullable
		public String getDomain() {
			return this.responseCookie.getDomain();
		}

		@Override
		@Nullable
		public String getPath() {
			return this.responseCookie.getPath();
		}

		@Override
		public boolean isSecure() {
			return this.responseCookie.isSecure();
		}

		@Nullable
		@Override
		public SameSite getSameSite() {
			// Adding non-null return site breaks tests.
			return null;
		}

		@Override
		public boolean isHttpOnly() {
			return this.responseCookie.isHttpOnly();
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

	private static class ContentWriterIteratingCallback extends IteratingCallback {
		private final SeekableByteChannel source;

		private final Content.Sink sink;

		private final Callback callback;

		private final RetainableByteBuffer buffer;

		private final long length;

		private long totalRead = 0;

		public ContentWriterIteratingCallback(SeekableByteChannel content, long position, long count, Response target, Callback callback) throws IOException {
			this.source = content;
			this.sink = target;
			this.callback = callback;
			this.length = count;
			this.source.position(position);

			ByteBufferPool bufferPool = target.getRequest().getComponents().getByteBufferPool();
			int outputBufferSize = target.getRequest().getConnectionMetaData().getHttpConfiguration().getOutputBufferSize();
			boolean useOutputDirectByteBuffers = target.getRequest().getConnectionMetaData().getHttpConfiguration().isUseOutputDirectByteBuffers();
			this.buffer = bufferPool.acquire(outputBufferSize, useOutputDirectByteBuffers);
		}

		@Override
		protected Action process() throws Throwable {
			if (!this.source.isOpen() || this.totalRead == this.length) {
				return Action.SUCCEEDED;
			}

			ByteBuffer byteBuffer = this.buffer.getByteBuffer();
			BufferUtil.clearToFill(byteBuffer);
			byteBuffer.limit((int) Math.min(this.buffer.capacity(), this.length - this.totalRead));
			int read = this.source.read(byteBuffer);
			if (read == -1) {
				IO.close(this.source);
				this.sink.write(true, BufferUtil.EMPTY_BUFFER, this);
				return Action.SCHEDULED;
			}
			this.totalRead += read;
			BufferUtil.flipToFlush(byteBuffer, 0);
			this.sink.write(false, byteBuffer, this);
			return Action.SCHEDULED;
		}

		@Override
		protected void onCompleteSuccess() {
			this.buffer.release();
			IO.close(this.source);
			this.callback.succeeded();
		}

		@Override
		protected void onCompleteFailure(Throwable x) {
			this.buffer.release();
			IO.close(this.source);
			this.callback.failed(x);
		}
	}
}
