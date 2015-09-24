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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.rx.Streams;

/**
 * @author Sebastien Deleuze
 */
public class CompletableFutureUtilsTests {

	private CountDownLatch lock = new CountDownLatch(1);
	private final List<Boolean> results = new ArrayList<>();
	private final List<Throwable> errors = new ArrayList<>();

	@Test
	public void fromPublisher() throws InterruptedException {
		Publisher<Boolean> publisher = Streams.just(true, false);
		CompletableFuture<List<Boolean>> future = CompletableFutureUtils.fromPublisher(publisher);
		future.whenComplete((result, error) -> {
			if (error != null) {
				errors.add(error);
			}
			else {
				results.addAll(result);
			}
			lock.countDown();
		});
		lock.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("onError not expected: " + errors.toString(), 0, errors.size());
		assertEquals(2, results.size());
		assertTrue(results.get(0));
		assertFalse(results.get(1));
	}

	@Test
	public void fromSinglePublisher() throws InterruptedException {
		Publisher<Boolean> publisher = Streams.just(true);
		CompletableFuture<Boolean> future = CompletableFutureUtils.fromSinglePublisher(publisher);
		future.whenComplete((result, error) -> {
			if (error != null) {
				errors.add(error);
			}
			else {
				results.add(result);
			}
			lock.countDown();
		});
		lock.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("onError not expected: " + errors.toString(), 0, errors.size());
		assertEquals(1, results.size());
		assertTrue(results.get(0));
	}

	@Test
	public void fromSinglePublisherWithMultipleValues() throws InterruptedException {
		Publisher<Boolean> publisher = Streams.just(true, false);
		CompletableFuture<Boolean> future = CompletableFutureUtils.fromSinglePublisher(publisher);
		future.whenComplete((result, error) -> {
			if (error != null) {
				errors.add(error);
			}
			else {
				results.add(result);
			}
			lock.countDown();
		});
		lock.await(2000, TimeUnit.MILLISECONDS);
		assertEquals(1, errors.size());
		assertEquals(IllegalStateException.class, errors.get(0).getClass());
		assertEquals(0, results.size());
	}

}