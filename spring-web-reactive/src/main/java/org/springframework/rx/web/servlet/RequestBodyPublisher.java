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

package org.springframework.rx.web.servlet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Arjen Poutsma
 */
public class RequestBodyPublisher implements ReadListener, Publisher<byte[]> {

	private final Charset UTF_8 = Charset.forName("UTF-8");

	private static final Log logger = LogFactory.getLog(RequestBodyPublisher.class);

	private final AsyncContextSynchronizer synchronizer;

	private final byte[] buffer;

	private long demand;

	private Subscriber<? super byte[]> subscriber;

	public RequestBodyPublisher(AsyncContextSynchronizer synchronizer, int bufferSize) {
		this.synchronizer = synchronizer;
		this.buffer = new byte[bufferSize];
	}

	@Override
	public void subscribe(Subscriber<? super byte[]> s) {
		this.subscriber = s;

		this.subscriber.onSubscribe(new RequestBodySubscription());
	}

	@Override
	public void onDataAvailable() throws IOException {
		ServletInputStream input = this.synchronizer.getInputStream();

		while (true) {
			logger.debug("Demand: " + this.demand);

			if (demand <= 0) {
				break;
			}

			boolean ready = input.isReady();
			logger.debug("Input " + ready + "/" + input.isFinished());

			if (!ready) {
				break;
			}

			int read = input.read(buffer);
			logger.debug("Input read:" + read);

			if (read == -1) {
				break;
			}
			else if (read > 0) {
				if (demand != Long.MAX_VALUE) {
					demand--;
				}
				byte[] copy = Arrays.copyOf(this.buffer, read);

//				logger.debug("Next: " + new String(copy, UTF_8));

				this.subscriber.onNext(copy);

			}
		}
	}

	@Override
	public void onAllDataRead() throws IOException {
		logger.debug("All data read");
		this.synchronizer.readComplete();
		this.subscriber.onComplete();
	}

	@Override
	public void onError(Throwable t) {
		logger.error("RequestBodyPublisher Error", t);
		this.subscriber.onError(t);
	}

	private class RequestBodySubscription implements Subscription {

		@Override
		public void request(long n) {
			logger.debug("Updating demand " + demand + " by " + n);

			boolean stalled = demand <= 0;

			if (n != Long.MAX_VALUE && demand != Long.MAX_VALUE) {
				demand += n;
			}
			else {
				demand = Long.MAX_VALUE;
			}

			if (stalled) {
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
			synchronizer.readComplete();
			demand = 0;
		}
	}
}
