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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Arjen Poutsma
 */
public class BlockingByteBufQueuePublisherTests {

	private BlockingSignalQueue queue;

	private BlockingSignalQueuePublisher publisher;

	@Before
	public void setUp() throws Exception {
		queue = new BlockingSignalQueue();
		publisher = new BlockingSignalQueuePublisher(queue);
	}

	@Test
	public void normal() throws Exception {
		ByteBuf abc = Unpooled.copiedBuffer(new byte[]{'a', 'b', 'c'});
		ByteBuf def = Unpooled.copiedBuffer(new byte[]{'d', 'e', 'f'});

		queue.putSignal(abc);
		queue.putSignal(def);
		queue.complete();

		final AtomicBoolean complete = new AtomicBoolean(false);
		final List<ByteBuf> received = new ArrayList<ByteBuf>(2);

		publisher.subscribe(new Subscriber<ByteBuf>() {
			@Override
			public void onSubscribe(Subscription s) {
				s.request(2);
			}

			@Override
			public void onNext(ByteBuf byteBuf) {
				received.add(byteBuf);
			}

			@Override
			public void onError(Throwable t) {
				fail("onError not expected");
			}

			@Override
			public void onComplete() {
				complete.set(true);
			}
		});

		while (!complete.get()) {
		}

		assertEquals(2, received.size());
		assertSame(abc, received.get(0));
		assertSame(def, received.get(1));
	}

	@Test
	public void unbounded() throws Exception {
		ByteBuf abc = Unpooled.copiedBuffer(new byte[]{'a', 'b', 'c'});
		ByteBuf def = Unpooled.copiedBuffer(new byte[]{'d', 'e', 'f'});

		queue.putSignal(abc);
		queue.putSignal(def);
		queue.complete();

		final AtomicBoolean complete = new AtomicBoolean(false);
		final List<ByteBuf> received = new ArrayList<ByteBuf>(2);

		publisher.subscribe(new Subscriber<ByteBuf>() {
			@Override
			public void onSubscribe(Subscription s) {
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(ByteBuf byteBuf) {
				received.add(byteBuf);
			}

			@Override
			public void onError(Throwable t) {
				fail("onError not expected");
			}

			@Override
			public void onComplete() {
				complete.set(true);
			}
		});

		while (!complete.get()) {
		}

		assertEquals(2, received.size());
		assertSame(abc, received.get(0));
		assertSame(def, received.get(1));
	}

	@Test
	public void multipleSubscribe() throws Exception {
		publisher.subscribe(new Subscriber<ByteBuf>() {
			@Override
			public void onSubscribe(Subscription s) {

			}

			@Override
			public void onNext(ByteBuf byteBuf) {

			}

			@Override
			public void onError(Throwable t) {

			}

			@Override
			public void onComplete() {

			}
		});
		publisher.subscribe(new Subscriber<ByteBuf>() {
			@Override
			public void onSubscribe(Subscription s) {
				fail("onSubscribe not expected");
			}

			@Override
			public void onNext(ByteBuf byteBuf) {
				fail("onNext not expected");
			}

			@Override
			public void onError(Throwable t) {
				assertTrue(t instanceof IllegalStateException);
			}

			@Override
			public void onComplete() {
				fail("onComplete not expected");
			}
		});

	}


}