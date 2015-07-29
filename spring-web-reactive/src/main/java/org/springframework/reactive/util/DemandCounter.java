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

package org.springframework.reactive.util;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.Assert;

/**
 * Small utility class for keeping track of Reactive Streams demand.
 * @author Arjen Poutsma
 */
public final class DemandCounter {

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
