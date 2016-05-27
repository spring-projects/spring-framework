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

package org.springframework.core.codec.support;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Mark Paluch
 */
public class StringDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private StringDecoder decoder;

	@Before
	public void createEncoder() {
		this.decoder = new StringDecoder();
	}


	@Test
	public void canDecode() {
		assertTrue(this.decoder
				.canDecode(ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN));
		assertTrue(this.decoder
				.canDecode(ResolvableType.forClass(String.class), MediaType.TEXT_HTML));
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(String.class),
				MediaType.APPLICATION_JSON));
		assertFalse(this.decoder
				.canDecode(ResolvableType.forClass(Integer.class), MediaType.TEXT_PLAIN));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() throws InterruptedException {
		this.decoder = new StringDecoder(false);
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("foo"), stringBuffer("bar"), stringBuffer("baz"));
		Flux<String> output =
				this.decoder.decode(source, ResolvableType.forClass(String.class), null);
		TestSubscriber<String> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(output).
				assertNoError().
				assertComplete().
				assertValues("foo", "bar", "baz");
	}

	@Test
	public void decodeNewLine() throws InterruptedException {
		DataBuffer fooBar = stringBuffer("\nfoo\r\nbar\r");
		DataBuffer baz = stringBuffer("\nbaz");
		Flux<DataBuffer> source = Flux.just(fooBar, baz);
		Flux<String> output =
				decoder.decode(source, ResolvableType.forClass(String.class), null);
		TestSubscriber<String> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(output).
				assertNoError().
				assertComplete().
				assertValues("foo", "bar", "baz");
	}


	@Test
	public void decodeEmpty() throws InterruptedException {
		Flux<DataBuffer> source = Flux.just(stringBuffer(""));
		Flux<String> output =
				this.decoder.decode(source, ResolvableType.forClass(String.class), null);
		TestSubscriber<String> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(output).assertValues("");
	}

}
