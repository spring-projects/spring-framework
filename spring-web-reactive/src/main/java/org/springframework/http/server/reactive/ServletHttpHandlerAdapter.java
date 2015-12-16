/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
@WebServlet(asyncSupported = true)
public class ServletHttpHandlerAdapter extends HttpServlet {

	private static final int BUFFER_SIZE = 8192;

	private static Log logger = LogFactory.getLog(ServletHttpHandlerAdapter.class);


	private HttpHandler handler;


	public void setHandler(HttpHandler handler) {
		this.handler = handler;
	}


	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		AsyncContext context = servletRequest.startAsync();
		ServletAsyncContextSynchronizer synchronizer = new ServletAsyncContextSynchronizer(context);

		RequestBodyPublisher requestBody = new RequestBodyPublisher(synchronizer, BUFFER_SIZE);
		ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest, requestBody);
		servletRequest.getInputStream().setReadListener(requestBody);

		ResponseBodySubscriber responseBodySubscriber = new ResponseBodySubscriber(synchronizer);
		ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse,
				publisher -> Mono.from(subscriber -> publisher.subscribe(responseBodySubscriber)));
		servletResponse.getOutputStream().setWriteListener(responseBodySubscriber);

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber(synchronizer, response);
		this.handler.handle(request, response).subscribe(resultSubscriber);
	}


	private static class RequestBodyPublisher implements ReadListener, Publisher<ByteBuffer> {

		private final ServletAsyncContextSynchronizer synchronizer;

		private final byte[] buffer;

		private final DemandCounter demand = new DemandCounter();

		private Subscriber<? super ByteBuffer> subscriber;

		private boolean stalled;

		private boolean cancelled;


		public RequestBodyPublisher(ServletAsyncContextSynchronizer synchronizer, int bufferSize) {
			this.synchronizer = synchronizer;
			this.buffer = new byte[bufferSize];
		}


		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			if (subscriber == null) {
				throw new NullPointerException();
			}
			else if (this.subscriber != null) {
				subscriber.onError(new IllegalStateException("Only one subscriber allowed"));
			}
			this.subscriber = subscriber;
			this.subscriber.onSubscribe(new RequestBodySubscription());
		}

		@Override
		public void onDataAvailable() throws IOException {
			if (cancelled) {
				return;
			}
			ServletInputStream input = this.synchronizer.getInputStream();
			logger.debug("onDataAvailable: " + input);

			while (true) {
				logger.debug("Demand: " + this.demand);

				if (!demand.hasDemand()) {
					stalled = true;
					break;
				}

				boolean ready = input.isReady();
				logger.debug("Input ready: " + ready + " finished: " + input.isFinished());

				if (!ready) {
					break;
				}

				int read = input.read(buffer);
				logger.debug("Input read:" + read);

				if (read == -1) {
					break;
				}
				else if (read > 0) {
					this.demand.decrement();
					byte[] copy = Arrays.copyOf(this.buffer, read);

//				logger.debug("Next: " + new String(copy, UTF_8));

					this.subscriber.onNext(ByteBuffer.wrap(copy));

				}
			}
		}

		@Override
		public void onAllDataRead() throws IOException {
			if (cancelled) {
				return;
			}
			logger.debug("All data read");
			this.synchronizer.readComplete();
			if (this.subscriber != null) {
				this.subscriber.onComplete();
			}
		}

		@Override
		public void onError(Throwable t) {
			if (cancelled) {
				return;
			}
			logger.error("RequestBodyPublisher Error", t);
			this.synchronizer.readComplete();
			if (this.subscriber != null) {
				this.subscriber.onError(t);
			}
		}

		private class RequestBodySubscription implements Subscription {

			@Override
			public void request(long n) {
				if (cancelled) {
					return;
				}
				logger.debug("Updating demand " + demand + " by " + n);

				demand.increase(n);

				logger.debug("Stalled: " + stalled);

				if (stalled) {
					stalled = false;
					try {
						onDataAvailable();
					}
					catch (IOException ex) {
						onError(ex);
					}
				}
			}

			@Override
			public void cancel() {
				if (cancelled) {
					return;
				}
				cancelled = true;
				synchronizer.readComplete();
				demand.reset();
			}
		}


		/**
		 * Small utility class for keeping track of Reactive Streams demand.
		 */
		private static final class DemandCounter {

			private final AtomicLong demand = new AtomicLong();

			/**
			 * Increases the demand by the given number
			 * @param n the positive number to increase demand by
			 * @return the increased demand
			 * @see org.reactivestreams.Subscription#request(long)
			 */
			public long increase(long n) {
				Assert.isTrue(n > 0, "'n' must be higher than 0");
				return demand.updateAndGet(d -> d != Long.MAX_VALUE ? d + n : Long.MAX_VALUE);
			}

			/**
			 * Decreases the demand by one.
			 * @return the decremented demand
			 */
			public long decrement() {
				return demand.updateAndGet(d -> d != Long.MAX_VALUE ? d - 1 : Long.MAX_VALUE);
			}

			/**
			 * Indicates whether this counter has demand, i.e. whether it is higher than 0.
			 * @return {@code true} if this counter has demand; {@code false} otherwise
			 */
			public boolean hasDemand() {
				return this.demand.get() > 0;
			}

			/**
			 * Resets this counter to 0.
			 * @see org.reactivestreams.Subscription#cancel()
			 */
			public void reset() {
				this.demand.set(0);
			}

			@Override
			public String toString() {
				return demand.toString();
			}
		}
	}

	private static class ResponseBodySubscriber implements WriteListener, Subscriber<ByteBuffer> {

		private final ServletAsyncContextSynchronizer synchronizer;

		private Subscription subscription;

		private ByteBuffer buffer;

		private volatile boolean subscriberComplete = false;


		public ResponseBodySubscriber(ServletAsyncContextSynchronizer synchronizer) {
			this.synchronizer = synchronizer;
		}


		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(ByteBuffer bytes) {

			Assert.isNull(buffer);

			this.buffer = bytes;
			try {
				onWritePossible();
			}
			catch (IOException e) {
				onError(e);
			}
		}

		@Override
		public void onComplete() {
			logger.debug("Complete buffer: " + (buffer == null));

			this.subscriberComplete = true;

			if (buffer == null) {
				this.synchronizer.writeComplete();
			}
		}

		@Override
		public void onWritePossible() throws IOException {
			ServletOutputStream output = this.synchronizer.getOutputStream();

			boolean ready = output.isReady();
			logger.debug("Output: " + ready + " buffer: " + (buffer == null));

			if (ready) {
				if (this.buffer != null) {
					byte[] bytes = new byte[this.buffer.remaining()];
					this.buffer.get(bytes);
					this.buffer = null;
					output.write(bytes);
					if (!subscriberComplete) {
						this.subscription.request(1);
					}
					else {
						this.synchronizer.writeComplete();
					}
				}
				else {
					this.subscription.request(1);
				}
			}
		}

		@Override
		public void onError(Throwable t) {
			logger.error("ResponseBodySubscriber error", t);
		}
	}

	private static class HandlerResultSubscriber implements Subscriber<Void> {

		private final ServletAsyncContextSynchronizer synchronizer;

		private final ServletServerHttpResponse response;


		public HandlerResultSubscriber(ServletAsyncContextSynchronizer synchronizer,
				ServletServerHttpResponse response) {

			this.synchronizer = synchronizer;
			this.response = response;
		}


		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
			// no op
		}

		@Override
		public void onError(Throwable ex) {
			logger.error("Error from request handling. Completing the request.", ex);
			this.response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			this.synchronizer.complete();
		}

		@Override
		public void onComplete() {
			this.synchronizer.complete();
		}
	}
}
