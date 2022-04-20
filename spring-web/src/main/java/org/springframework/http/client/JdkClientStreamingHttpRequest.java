/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link ClientHttpRequest} implementation based on
 * JDK HTTP client in streaming mode.
 *
 * <p>Created via the {@link JdkClientHttpRequestFactory}.
 */
final class JdkClientStreamingHttpRequest extends AbstractClientHttpRequest
		implements StreamingHttpOutputMessage {

	private final HttpClient httpClient;

	private final HttpMethod method;

	private final URI uri;

	private final boolean expectContinue;

	@Nullable
	private final Duration requestTimeout;

	@Nullable
	private Body body;

	JdkClientStreamingHttpRequest(HttpClient client, HttpMethod method, URI uri,
								  boolean expectContinue, @Nullable Duration requestTimeout) {
		this.httpClient = client;
		this.method = method;
		this.uri = uri;
		this.expectContinue = expectContinue;
		this.requestTimeout = requestTimeout;
	}

	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	@Deprecated
	public String getMethodValue() {
		return this.method.name();
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public void setBody(Body body) {
		assertNotExecuted();
		this.body = body;
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		throw new UnsupportedOperationException("getBody not supported");
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(this.uri);

		JdkClientHttpRequest.addHeaders(builder, headers);

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<SubscriptionOutputStream> reference;
		if (this.body != null) {
			reference = new AtomicReference<>();
			builder.method(this.method.name(), HttpRequest.BodyPublishers.fromPublisher(subscriber -> {
				SubscriptionOutputStream outputStream = new SubscriptionOutputStream(subscriber);
				reference.set(outputStream);
				latch.countDown();
				try {
					subscriber.onSubscribe(outputStream);
				} catch (Throwable t) {
					outputStream.closed = true;
					throw t;
				}
			}));
		} else {
			reference = null;
			builder.method(this.method.name(), HttpRequest.BodyPublishers.noBody());
		}

		if (expectContinue) {
			builder.expectContinue(true);
		}
		if (requestTimeout != null) {
			builder.timeout(requestTimeout);
		}

		HttpResponse<InputStream> response;
		try {
			if (this.body != null) {
				CompletableFuture<HttpResponse<InputStream>> future = this.httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
				latch.await();
				SubscriptionOutputStream outputStream = reference.get();
				try (outputStream) {
					this.body.writeTo(outputStream);
				} catch (Throwable t) {
					outputStream.cancel();
					outputStream.subscriber.onError(t);
				}
				response = future.join();
			} else {
				response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());;
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return new JdkClientHttpResponse(response);
	}

	static class SubscriptionOutputStream extends OutputStream implements Flow.Subscription {

		private final Flow.Subscriber<? super ByteBuffer> subscriber;

		private final Semaphore semaphore = new Semaphore(0);

		private volatile boolean closed;

		SubscriptionOutputStream(Flow.Subscriber<? super ByteBuffer> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public void write(byte[] b) throws IOException {;
			if (acquire()) {
				subscriber.onNext(ByteBuffer.wrap(b));
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (acquire()) {
				subscriber.onNext(ByteBuffer.wrap(b, off, len));
			}
		}

		@Override
		public void write(int b) throws IOException {
			if (acquire()) {
				subscriber.onNext(ByteBuffer.wrap(new byte[] {(byte) b}));
			}
		}

		@Override
		public void close() throws IOException {
			if (!closed) {
				closed = true;
				subscriber.onComplete();
			}
		}

		private boolean acquire() throws IOException {
			if (closed) {
				throw new IOException("closed");
			}
			try {
				semaphore.acquire();
				return true;
			} catch (InterruptedException e) {
				closed = true;
				subscriber.onError(e);
				return false;
			}
		}

		@Override
		public void request(long n) {
			semaphore.release((int) n);
		}

		@Override
		public void cancel() {
			closed = true;
			semaphore.release(1);
		}
	}
}
