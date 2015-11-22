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

package org.springframework.http.server.servlet31;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public class RequestBodyPublisher implements ReadListener, Publisher<ByteBuffer> {

	private static final Log logger = LogFactory.getLog(RequestBodyPublisher.class);

	private final AsyncContextSynchronizer synchronizer;

	private final byte[] buffer;

	private final DemandCounter demand = new DemandCounter();

	private Subscriber<? super ByteBuffer> subscriber;

	private boolean stalled;

	private boolean cancelled;

	public RequestBodyPublisher(AsyncContextSynchronizer synchronizer, int bufferSize) {
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
