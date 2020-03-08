/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.function.Consumer;

import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.http.MediaType;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ProtobufEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class ProtobufEncoderTests extends AbstractEncoderTests<ProtobufEncoder> {

	private final static MimeType PROTOBUF_MIME_TYPE = new MimeType("application", "x-protobuf");

	private Msg msg1 =
			Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	private Msg msg2 =
			Msg.newBuilder().setFoo("Bar").setBlah(SecondMsg.newBuilder().setBlah(456).build()).build();


	public ProtobufEncoderTests() {
		super(new ProtobufEncoder());
	}

	@Override
	@Test
	public void canEncode() {
		assertThat(this.encoder.canEncode(forClass(Msg.class), null)).isTrue();
		assertThat(this.encoder.canEncode(forClass(Msg.class), PROTOBUF_MIME_TYPE)).isTrue();
		assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_OCTET_STREAM)).isTrue();
		assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_JSON)).isFalse();
		assertThat(this.encoder.canEncode(forClass(Object.class), PROTOBUF_MIME_TYPE)).isFalse();
	}

	@Override
	@Test
	public void encode() {
		Mono<Message> input = Mono.just(this.msg1);

		testEncodeAll(input, Msg.class, step -> step
				.consumeNextWith(dataBuffer -> {
					try {
						assertThat(Msg.parseFrom(dataBuffer.asInputStream())).isEqualTo(this.msg1);

					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
					finally {
						DataBufferUtils.release(dataBuffer);
					}
				})
				.verifyComplete());
	}

	@Test
	public void encodeStream() {
		Flux<Message> input = Flux.just(this.msg1, this.msg2);

		testEncodeAll(input, Msg.class, step -> step
				.consumeNextWith(expect(this.msg1))
				.consumeNextWith(expect(this.msg2))
				.verifyComplete());
	}

	protected final Consumer<DataBuffer> expect(Msg msg) {
		return dataBuffer -> {
			try {
				assertThat(Msg.parseDelimitedFrom(dataBuffer.asInputStream())).isEqualTo(msg);

			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		};
	}
}
