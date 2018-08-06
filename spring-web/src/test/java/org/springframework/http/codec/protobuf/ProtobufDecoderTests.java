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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ProtobufDecoder}.
 * TODO Make tests more readable
 * TODO Add a test where an input DataBuffer is larger than a message
 *
 * @author Sebastien Deleuze
 */
public class ProtobufDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private final static MimeType PROTOBUF_MIME_TYPE = new MimeType("application", "x-protobuf");

	private final Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

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
		byte[] body = this.testMsg.toByteArray();
		Flux<DataBuffer> source = Flux.just(this.bufferFactory.wrap(body));
		ResolvableType elementType = forClass(Msg.class);
		Mono<Message> mono = this.decoder.decodeToMono(source, elementType, null,
				emptyMap());

		StepVerifier.create(mono)
				.expectNext(this.testMsg)
				.verifyComplete();
	}

	@Test
	public void decodeChunksToMono() {
		byte[] body = this.testMsg.toByteArray();
		List<DataBuffer> chunks = new ArrayList<>();
		chunks.add(this.bufferFactory.wrap(Arrays.copyOfRange(body, 0, 4)));
		chunks.add(this.bufferFactory.wrap(Arrays.copyOfRange(body, 4, body.length)));
		Flux<DataBuffer> source = Flux.fromIterable(chunks);
		ResolvableType elementType = forClass(Msg.class);
		Mono<Message> mono = this.decoder.decodeToMono(source, elementType, null,
				emptyMap());

		StepVerifier.create(mono)
				.expectNext(this.testMsg)
				.verifyComplete();
	}

	@Test
	public void decode() throws IOException {
		Msg testMsg2 = Msg.newBuilder().setFoo("Bar").setBlah(SecondMsg.newBuilder().setBlah(456).build()).build();

		DataBuffer buffer = bufferFactory.allocateBuffer();
		OutputStream outputStream = buffer.asOutputStream();
		this.testMsg.writeDelimitedTo(outputStream);

		DataBuffer buffer2 = bufferFactory.allocateBuffer();
		OutputStream outputStream2 = buffer2.asOutputStream();
		testMsg2.writeDelimitedTo(outputStream2);

		Flux<DataBuffer> source = Flux.just(buffer, buffer2);
		ResolvableType elementType = forClass(Msg.class);
		Flux<Message> messages = this.decoder.decode(source, elementType, null, emptyMap());

		StepVerifier.create(messages)
				.expectNext(this.testMsg)
				.expectNext(testMsg2)
				.verifyComplete();

		DataBufferUtils.release(buffer);
		DataBufferUtils.release(buffer2);
	}

	@Test
	public void decodeChunks() throws IOException {
		Msg testMsg2 = Msg.newBuilder().setFoo("Bar").setBlah(SecondMsg.newBuilder().setBlah(456).build()).build();
		List<DataBuffer> chunks = new ArrayList<>();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		this.testMsg.writeDelimitedTo(outputStream);
		byte[] byteArray = outputStream.toByteArray();
		ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
		testMsg2.writeDelimitedTo(outputStream2);
		byte[] byteArray2 = outputStream2.toByteArray();

		chunks.add(this.bufferFactory.wrap(Arrays.copyOfRange(byteArray, 0, 4)));
		byte[] chunk2 = Arrays.copyOfRange(byteArray, 4, byteArray.length);
		byte[] chunk3 = Arrays.copyOfRange(byteArray2, 0, 4);
		byte[] combined = new byte[chunk2.length + chunk3.length];
		for (int i = 0; i < combined.length; ++i)
		{
			combined[i] = i < chunk2.length ? chunk2[i] : chunk3[i - chunk2.length];
		}
		chunks.add(this.bufferFactory.wrap(combined));
		chunks.add(this.bufferFactory.wrap(Arrays.copyOfRange(byteArray2, 4, byteArray2.length)));

		Flux<DataBuffer> source = Flux.fromIterable(chunks);
		ResolvableType elementType = forClass(Msg.class);
		Flux<Message> messages = this.decoder.decode(source, elementType, null, emptyMap());

		StepVerifier.create(messages)
				.expectNext(this.testMsg)
				.expectNext(testMsg2)
				.verifyComplete();
	}

	@Test
	public void exceedMaxSize() {
		this.decoder.setMaxMessageSize(1);
		byte[] body = this.testMsg.toByteArray();
		Flux<DataBuffer> source = Flux.just(this.bufferFactory.wrap(body));
		ResolvableType elementType = forClass(Msg.class);
		Flux<Message> messages = this.decoder.decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(messages)
				.verifyError(DecodingException.class);
	}

}
