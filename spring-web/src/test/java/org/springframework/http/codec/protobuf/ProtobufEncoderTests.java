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

package org.springframework.http.codec.protobuf;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.google.protobuf.Message;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;
import org.springframework.util.MimeType;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ProtobufEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class ProtobufEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private final static MimeType PROTOBUF_MIME_TYPE = new MimeType("application", "x-protobuf");

	private final Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	private final ProtobufEncoder encoder = new ProtobufEncoder();

	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(forClass(Msg.class), null));
		assertTrue(this.encoder.canEncode(forClass(Msg.class), PROTOBUF_MIME_TYPE));
		assertTrue(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_OCTET_STREAM));
		assertFalse(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_JSON));
		assertFalse(this.encoder.canEncode(forClass(Object.class), PROTOBUF_MIME_TYPE));
	}

	@Test
	public void encode() {
		Mono<Message> message = Mono.just(this.testMsg);
		ResolvableType elementType = forClass(Msg.class);
		Flux<DataBuffer> output = this.encoder.encode(message, this.bufferFactory, elementType, PROTOBUF_MIME_TYPE, emptyMap());
		StepVerifier.create(output)
				.consumeNextWith(dataBuffer -> {
					try {
						assertEquals(this.testMsg, Msg.parseFrom(dataBuffer.asInputStream()));
						DataBufferUtils.release(dataBuffer);
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				})
				.verifyComplete();
	}

	@Test
	public void encodeStream() {
		Msg testMsg2 = Msg.newBuilder().setFoo("Bar").setBlah(SecondMsg.newBuilder().setBlah(456).build()).build();
		Flux<Message> messages = Flux.just(this.testMsg, testMsg2);
		ResolvableType elementType = forClass(Msg.class);
		Flux<DataBuffer> output = this.encoder.encode(messages, this.bufferFactory, elementType, PROTOBUF_MIME_TYPE, emptyMap());
		StepVerifier.create(output)
				.consumeNextWith(dataBuffer -> {
					try {
						assertEquals(this.testMsg, Msg.parseDelimitedFrom(dataBuffer.asInputStream()));
						DataBufferUtils.release(dataBuffer);
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				})
				.consumeNextWith(dataBuffer -> {
					try {
						assertEquals(testMsg2, Msg.parseDelimitedFrom(dataBuffer.asInputStream()));
						DataBufferUtils.release(dataBuffer);
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				})
				.verifyComplete();
	}

}
