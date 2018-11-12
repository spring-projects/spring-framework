/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.codec;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class DataBufferEncoderTests extends AbstractEncoderTestCase<DataBuffer, DataBufferEncoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

	public DataBufferEncoderTests() {
		super(new DataBufferEncoder(), DataBuffer.class);
	}

	@Override
	protected Flux<DataBuffer> input() {
//		DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
		return Flux.just(this.fooBytes, this.barBytes)
				.map(bytes -> {
					DataBuffer dataBuffer = bufferFactory.allocateBuffer(bytes.length);
					dataBuffer.write(bytes);
					return dataBuffer;
				});
	}

	@Override
	protected Stream<Consumer<DataBuffer>> outputConsumers() {
		return Stream.<Consumer<DataBuffer>>builder()
				.add(resultConsumer(this.fooBytes))
				.add(resultConsumer(this.barBytes))
				.build();

	}

	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(DataBuffer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(DataBuffer.class),
				MimeTypeUtils.APPLICATION_JSON));

		// SPR-15464
		assertFalse(this.encoder.canEncode(ResolvableType.NONE, null));
	}

}
