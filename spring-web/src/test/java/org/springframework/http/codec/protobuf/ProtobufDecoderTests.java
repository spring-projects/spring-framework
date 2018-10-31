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

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;
import org.springframework.util.MimeType;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ProtobufDecoder}.
 *
 * @author Sebastien Deleuze
 */
public class ProtobufDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private final static MimeType PROTOBUF_MIME_TYPE = new MimeType("application", "x-protobuf");

	private final SecondMsg secondMsg = SecondMsg.newBuilder().setBlah(123).build();

	private final Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(secondMsg).build();

	private final SecondMsg secondMsg2 = SecondMsg.newBuilder().setBlah(456).build();

	private final Msg testMsg2 = Msg.newBuilder().setFoo("Bar").setBlah(secondMsg2).build();

	private ProtobufDecoder decoder;


	@Before
	public void setup() {
		this.decoder = new ProtobufDecoder();
	}

	@Test(expected = IllegalArgumentException.class)
	public void extensionRegistryNull() {
		new ProtobufDecoder(null);
	}

	@Test
	public void canDecode() {
		assertTrue(this.decoder.canDecode(forClass(Msg.class), null));
		assertTrue(this.decoder.canDecode(forClass(Msg.class), PROTOBUF_MIME_TYPE));
		assertTrue(this.decoder.canDecode(forClass(Msg.class), MediaType.APPLICATION_OCTET_STREAM));
		assertFalse(this.decoder.canDecode(forClass(Msg.class), MediaType.APPLICATION_JSON));
		assertFalse(this.decoder.canDecode(forClass(Object.class), PROTOBUF_MIME_TYPE));
	}

	@Test
	public void decodeToMono() {
		DataBuffer data = byteBuffer(testMsg.toByteArray());
		ResolvableType elementType = forClass(Msg.class);

		Mono<Message> mono = this.decoder.decodeToMono(Flux.just(data), elementType, null, emptyMap());

		StepVerifier.create(mono)
				.expectNext(testMsg)
				.verifyComplete();
	}

	@Test
	public void decodeToMonoWithLargerDataBuffer() {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(1024);
		buffer.write(testMsg.toByteArray());
		ResolvableType elementType = forClass(Msg.class);

		Mono<Message> mono = this.decoder.decodeToMono(Flux.just(buffer), elementType, null, emptyMap());

		StepVerifier.create(mono)
				.expectNext(testMsg)
				.verifyComplete();
	}

	@Test
	public void decodeChunksToMono() {
		DataBuffer buffer = byteBuffer(testMsg.toByteArray());
		Flux<DataBuffer> chunks = Flux.just(
				DataBufferUtils.retain(buffer.slice(0, 4)),
				DataBufferUtils.retain(buffer.slice(4, buffer.readableByteCount() - 4)));
		ResolvableType elementType = forClass(Msg.class);
		release(buffer);

		Mono<Message> mono = this.decoder.decodeToMono(chunks, elementType, null,
				emptyMap());

		StepVerifier.create(mono)
				.expectNext(testMsg)
				.verifyComplete();
	}

	@Test
	public void decode() throws IOException {
		DataBuffer buffer = this.bufferFactory.allocateBuffer();
		testMsg.writeDelimitedTo(buffer.asOutputStream());
		DataBuffer buffer2 = this.bufferFactory.allocateBuffer();
		testMsg2.writeDelimitedTo(buffer2.asOutputStream());

		Flux<DataBuffer> source = Flux.just(buffer, buffer2);
		ResolvableType elementType = forClass(Msg.class);

		Flux<Message> messages = this.decoder.decode(source, elementType, null, emptyMap());

		StepVerifier.create(messages)
				.expectNext(testMsg)
				.expectNext(testMsg2)
				.verifyComplete();
	}

	@Test
	public void decodeSplitChunks() throws IOException {
		DataBuffer buffer = this.bufferFactory.allocateBuffer();
		testMsg.writeDelimitedTo(buffer.asOutputStream());
		DataBuffer buffer2 = this.bufferFactory.allocateBuffer();
		testMsg2.writeDelimitedTo(buffer2.asOutputStream());

		Flux<DataBuffer> chunks = Flux.just(
				DataBufferUtils.retain(buffer.slice(0, 4)),
				DataBufferUtils.retain(buffer.slice(4, buffer.readableByteCount() - 4)),
				DataBufferUtils.retain(buffer2.slice(0, 2)),
				DataBufferUtils.retain(buffer2
						.slice(2, buffer2.readableByteCount() - 2)));
		release(buffer, buffer2);

		ResolvableType elementType = forClass(Msg.class);
		Flux<Message> messages = this.decoder.decode(chunks, elementType, null, emptyMap());

		StepVerifier.create(messages)
				.expectNext(testMsg)
				.expectNext(testMsg2)
				.verifyComplete();
	}

	@Test
	public void decodeMergedChunks() throws IOException {
		DataBuffer buffer = bufferFactory.allocateBuffer();
		testMsg.writeDelimitedTo(buffer.asOutputStream());
		testMsg.writeDelimitedTo(buffer.asOutputStream());

		ResolvableType elementType = forClass(Msg.class);
		Flux<Message> messages = this.decoder.decode(Mono.just(buffer), elementType, null, emptyMap());

		StepVerifier.create(messages)
				.expectNext(testMsg)
				.expectNext(testMsg)
				.verifyComplete();
	}

	@Test
	public void exceedMaxSize() {
		this.decoder.setMaxMessageSize(1);
		Flux<DataBuffer> source = Flux.just(byteBuffer(testMsg.toByteArray()));
		ResolvableType elementType = forClass(Msg.class);
		Flux<Message> messages = this.decoder.decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(messages)
				.verifyError(DecodingException.class);
	}

}
