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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapt an Eclipse Jetty {@link Response} to a {@link org.springframework.http.server.ServerHttpResponse}
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.2
 */
class JettyCoreServerHttpResponse implements ServerHttpResponse, ZeroCopyHttpOutputMessage
{
	private final AtomicBoolean committed = new AtomicBoolean(false);
	private final List<Supplier<? extends Mono<Void>>> commitActions = new CopyOnWriteArrayList<>();

	private final Response response;
	private final HttpHeaders headers;

	@Nullable
	private LinkedMultiValueMap<String, ResponseCookie> cookies;

	public JettyCoreServerHttpResponse(Response response) {
		this.response = response;
		headers = new HttpHeaders(new JettyHeadersAdapter(response.getHeaders()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return headers;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return DefaultDataBufferFactory.sharedInstance;
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		return response.isCommitted();
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body)
	{
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
		if (mono != null)
			return mono.then(Mono.defer(this::setComplete));

		Callback.Completable callback = new Callback.Completable();
		response.write(true, BufferUtil.EMPTY_BUFFER, callback);
		return Mono.fromFuture(callback);
	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count)
	{
		Mono<Void> mono = ensureCommitted();
		if (mono != null)
			return mono.then(Mono.defer(() -> writeWith(file, position, count)));

		Callback.Completable callback = new Callback.Completable();
		mono = Mono.fromFuture(callback);
		try
		{
			// TODO: Why does this say possible blocking call?
			SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
			new ContentWriterIteratingCallback(channel, position, count, response, callback).iterate();
		}
		catch (Throwable t)
		{
			callback.failed(t);
		}
		return mono;
	}

	private static class ContentWriterIteratingCallback extends IteratingCallback
	{
		private final SeekableByteChannel source;
		private final Content.Sink sink;
		private final Callback callback;
		private final RetainableByteBuffer buffer;
		private final long length;
		private long totalRead = 0;

		public ContentWriterIteratingCallback(SeekableByteChannel content, long position, long count, Response target, Callback callback) throws IOException
		{
			this.source = content;
			this.sink = target;
			this.callback = callback;
			this.length = count;
			source.position(position);

			ByteBufferPool bufferPool = target.getRequest().getComponents().getByteBufferPool();
			int outputBufferSize = target.getRequest().getConnectionMetaData().getHttpConfiguration().getOutputBufferSize();
			boolean useOutputDirectByteBuffers = target.getRequest().getConnectionMetaData().getHttpConfiguration().isUseOutputDirectByteBuffers();
			this.buffer = bufferPool.acquire(outputBufferSize, useOutputDirectByteBuffers);
		}

		@Override
		protected Action process() throws Throwable
		{
			if (!source.isOpen() || totalRead == length)
				return Action.SUCCEEDED;

			ByteBuffer byteBuffer = buffer.getByteBuffer();
			BufferUtil.clearToFill(byteBuffer);
			byteBuffer.limit((int)Math.min(buffer.capacity(), length - totalRead));
			int read = source.read(byteBuffer);
			if (read == -1)
			{
				IO.close(source);
				sink.write(true, BufferUtil.EMPTY_BUFFER, this);
				return Action.SCHEDULED;
			}
			totalRead += read;
			BufferUtil.flipToFlush(byteBuffer, 0);
			sink.write(false, byteBuffer, this);
			return Action.SCHEDULED;
		}

		@Override
		protected void onCompleteSuccess()
		{
			buffer.release();
			IO.close(source);
			callback.succeeded();
		}

		@Override
		protected void onCompleteFailure(Throwable x)
		{
			buffer.release();
			IO.close(source);
			callback.failed(x);
		}
	}

	@Nullable
	private Mono<Void> ensureCommitted()
	{
		if (committed.compareAndSet(false, true))
		{
			if (!this.commitActions.isEmpty())
			{
				return Flux.concat(Flux.fromIterable(this.commitActions).map(Supplier::get))
						.concatWith(Mono.fromRunnable(this::doCommit))
						.then()
						.doOnError(t -> getHeaders().clearContentHeaders());
			}

			doCommit();
		}

		return null;
	}

	private void doCommit()
	{
		if (cookies != null)
		{
			// TODO: are we doubling up on cookies already existing in response?
			cookies.values().stream()
					.flatMap(List::stream)
					.forEach(cookie ->
					{
						Response.addCookie(response, new HttpResponseCookie(cookie));
					});
		}
	}

	private Mono<Void> sendDataBuffer(DataBuffer dataBuffer) {
		Mono<Void> mono = ensureCommitted();
		if (mono != null)
			return mono.then(Mono.defer(() -> sendDataBuffer(dataBuffer)));

		@SuppressWarnings("resource")
		DataBuffer.ByteBufferIterator byteBufferIterator = dataBuffer.readableByteBuffers();
		Callback.Completable callback = new Callback.Completable();
		new IteratingCallback()
		{
			@Override
			protected Action process()
			{
				if (!byteBufferIterator.hasNext())
					return Action.SUCCEEDED;
				response.write(false, byteBufferIterator.next(), this);
				return Action.SCHEDULED;
			}

			@Override
			protected void onCompleteSuccess()
			{
				byteBufferIterator.close();
				callback.complete(null);
			}

			@Override
			protected void onCompleteFailure(Throwable cause)
			{
				byteBufferIterator.close();
				callback.failed(cause);
			}
		}.iterate();

		return Mono.fromFuture(callback);
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
		if (cookies == null)
			initializeCookies();
		return cookies;
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		if (cookies == null)
			initializeCookies();
		cookies.add(cookie.getName(), cookie);
	}

	private void initializeCookies()
	{
		cookies = new LinkedMultiValueMap<>();
		for (HttpField f : response.getHeaders()) {
			if (f instanceof HttpCookieUtils.SetCookieHttpField setCookieHttpField && setCookieHttpField.getHttpCookie() instanceof HttpResponseCookie httpResponseCookie)
				cookies.add(httpResponseCookie.getName(), httpResponseCookie.getResponseCookie());
		}
	}

	private static class HttpResponseCookie implements org.eclipse.jetty.http.HttpCookie {
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
			// Adding non-null return site breaks tests.
			return null;
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
