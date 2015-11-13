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

package org.springframework.web.reactive.server.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.io.buffer.Buffer;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public class ResponseBodySubscriber implements WriteListener, Subscriber<ByteBuffer> {

	private static final Log logger = LogFactory.getLog(ResponseBodySubscriber.class);

	private final AsyncContextSynchronizer synchronizer;

	private Subscription subscription;

	private ByteBuffer buffer;

	private volatile boolean subscriberComplete = false;

	public ResponseBodySubscriber(AsyncContextSynchronizer synchronizer) {
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
				output.write(new Buffer(this.buffer).asBytes());
				this.buffer = null;

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
