/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io.buffer.support;

import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;

import static org.junit.Assert.assertFalse;

/**
 * @author Arjen Poutsma
 */
public class DataBufferUtilsTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void readChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt")
				.toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);

		Flux<DataBuffer> flux = DataBufferUtils.read(channel, this.bufferFactory, 4);

		TestSubscriber
				.subscribe(flux)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(
						stringConsumer("foo\n"), stringConsumer("bar\n"),
						stringConsumer("baz\n"), stringConsumer("qux\n"));

		assertFalse(channel.isOpen());
	}

	@Test
	public void readUnalignedChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt")
				.toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);

		Flux<DataBuffer> flux = DataBufferUtils.read(channel, this.bufferFactory, 3);

		TestSubscriber
				.subscribe(flux)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(
						stringConsumer("foo"), stringConsumer("\nba"),
						stringConsumer("r\nb"), stringConsumer("az\n"),
						stringConsumer("qux"), stringConsumer("\n"));

		assertFalse(channel.isOpen());
	}

	@Test
	public void readInputStream() {
		InputStream is = DataBufferUtilsTests.class
				.getResourceAsStream("DataBufferUtilsTests.txt");

		Flux<DataBuffer> flux = DataBufferUtils.read(is, this.bufferFactory, 4);

		TestSubscriber
				.subscribe(flux)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(
						stringConsumer("foo\n"), stringConsumer("bar\n"),
						stringConsumer("baz\n"), stringConsumer("qux\n"));
	}

	@Test
	public void takeUntilByteCount() {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);

		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(flux, 5L);

		TestSubscriber
				.subscribe(result)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(stringConsumer("foo"), stringConsumer("ba"));

		release(baz);
	}

}