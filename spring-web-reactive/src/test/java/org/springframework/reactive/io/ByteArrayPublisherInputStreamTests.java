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

package org.springframework.reactive.io;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.rx.Stream;
import reactor.rx.Streams;

import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ByteArrayPublisherInputStreamTests {


	private ByteArrayPublisherInputStream is;

	@Before
	public void createStream() {
		Stream<byte[]> stream =
				Streams.just(new byte[]{'a', 'b', 'c'}, new byte[]{'d', 'e'});

		is = new ByteArrayPublisherInputStream(stream);
	}

	@Test
	public void reactor() throws Exception {
		assertEquals(3, is.available());

		int ch = is.read();
		assertEquals('a', ch);
		ch = is.read();
		assertEquals('b', ch);
		ch = is.read();
		assertEquals('c', ch);

		assertEquals(2, is.available());
		ch = is.read();
		assertEquals('d', ch);
		ch = is.read();
		assertEquals('e', ch);

		ch = is.read();
		assertEquals(-1, ch);

		assertEquals(0, is.available());
	}

	@Test
	public void copy() throws  Exception {
		ByteArrayPublisherOutputStream os = new ByteArrayPublisherOutputStream();

		FileCopyUtils.copy(is, os);

		Publisher<byte[]> publisher = os.toByteArrayPublisher();
		List<byte[]> result = new ArrayList<>();
		AtomicBoolean complete = new AtomicBoolean();

		publisher.subscribe(new Subscriber<byte[]>() {

			@Override
			public void onSubscribe(Subscription s) {
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(byte[] bytes) {
				result.add(bytes);
			}

			@Override
			public void onError(Throwable t) {
				fail(t.getMessage());
			}

			@Override
			public void onComplete() {
				complete.set(true);
			}
		});

		while (!complete.get()) {

		}
		assertArrayEquals(result.get(0), new byte[]{'a', 'b', 'c'});
		assertArrayEquals(result.get(1), new byte[]{'d', 'e'});

	}

}