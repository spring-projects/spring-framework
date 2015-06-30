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

package org.springframework.rx.util;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Arjen Poutsma
 */
public abstract class AbstractUnicastSyncSubscriber<T> implements Subscriber<T> {

	private Subscription subscription;

	private boolean done = false;

	@Override
	public final void onSubscribe(Subscription subscription) {
		if (subscription == null) {
			throw new NullPointerException();
		}

		if (this.subscription != null) {
			subscription.cancel();
		}
		else {
			this.subscription = subscription;
			this.subscription.request(1);
		}
	}

	@Override
	public final void onNext(T element) {
		if (element == null) {
			throw new NullPointerException();
		}

		if (!done) {
			try {
				if (onNextInternal(element)) {
					subscription.request(1);
				}
				else {
					done();
				}
			}
			catch (Throwable t) {
				done();
				onError(t);
			}
		}
	}

	private void done() {
		done = true;
		subscription.cancel();
	}

	protected abstract boolean onNextInternal(final T element) throws Exception;


}
