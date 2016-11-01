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

package org.springframework.core.io.buffer;

import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.subscriber.Verifier;

import static org.junit.Assert.assertFalse;

/**
 * @author Arjen Poutsma
 */
public class DataBufferUtilsTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void readChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);
		Flux<DataBuffer> flux = DataBufferUtils.read(channel, this.bufferFactory, 3);

		Verifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify();

		assertFalse(channel.isOpen());
	}

	@Test
	public void readUnalignedChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);
		Flux<DataBuffer> flux = DataBufferUtils.read(channel, this.bufferFactory, 5);

		Verifier.create(flux)
				.consumeNextWith(stringConsumer("fooba"))
				.consumeNextWith(stringConsumer("rbazq"))
				.consumeNextWith(stringConsumer("ux"))
				.expectComplete()
				.verify();

		assertFalse(channel.isOpen());
	}

	@Test
	public void readInputStream() throws Exception {
		InputStream is = DataBufferUtilsTests.class.getResourceAsStream("DataBufferUtilsTests.txt");
		Flux<DataBuffer> flux = DataBufferUtils.read(is, this.bufferFactory, 3);

		Verifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify();
	}

	@Test
	public void takeUntilByteCount() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(flux, 5L);

		Verifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("ba"))
				.expectComplete().verify();

		release(baz);
	}

	@Test
	public void skipUntilByteCount() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 5L);

		Verifier.create(result)
				.consumeNextWith(stringConsumer("r"))
				.consumeNextWith(stringConsumer("baz"))
				.expectComplete()
				.verify();
	}

	@Test
	public void skipUntilByteCountShouldSkipAll() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 9L);

		Verifier.create(result)
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

}
