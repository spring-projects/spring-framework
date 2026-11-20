/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.protobuf;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.http.MediaType;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProtobufJsonDecoder}.
 * @author Brian Clozel
 */
class ProtobufJsonDecoderTests extends AbstractDecoderTests<ProtobufJsonDecoder> {

	private Msg msg1 = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	public ProtobufJsonDecoderTests() {
		super(new ProtobufJsonDecoder());
	}

	@Test
	@Override
	protected void canDecode() throws Exception {
		ResolvableType msgType = ResolvableType.forClass(Msg.class);
		assertThat(this.decoder.canDecode(msgType, null)).isFalse();
		assertThat(this.decoder.canDecode(msgType, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.decoder.canDecode(msgType, MediaType.APPLICATION_PROTOBUF)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Object.class), MediaType.APPLICATION_JSON)).isFalse();
	}

	@Test
	@Override
	protected void decode() throws Exception {
		ResolvableType msgType = ResolvableType.forClass(Msg.class);
		Flux<DataBuffer> input = Flux.just(dataBuffer("[{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}"),
				dataBuffer(",{\"foo\":\"Bar\",\"blah\":{\"blah\":456}}"),
				dataBuffer("]"));

		testDecode(input, msgType, step -> step.consumeErrorWith(error -> assertThat(error).isInstanceOf(UnsupportedOperationException.class)),
				MediaType.APPLICATION_JSON, null);
	}

	@Test
	@Override
	protected void decodeToMono() throws Exception {
		DataBuffer dataBuffer = dataBuffer("{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}");
		testDecodeToMonoAll(Mono.just(dataBuffer), Msg.class, step -> step
				.expectNext(this.msg1)
				.verifyComplete());
	}

	@Test
	void decodeToMonoNonAsciiUtf8() {
		Msg expected = Msg.newBuilder().setFoo("Caf\u00e9 \u20ac").build();
		byte[] json = "{\"foo\":\"Caf\u00e9 \u20ac\"}".getBytes(StandardCharsets.UTF_8);

		testDecodeToMono(Mono.just(this.bufferFactory.wrap(json)), ResolvableType.forClass(Msg.class), step -> step
				.expectNext(expected)
				.verifyComplete(), MediaType.APPLICATION_JSON, null);
	}

	@Test
	void exceedMaxSize() {
		this.decoder.setMaxMessageSize(1);
		DataBuffer first = dataBuffer("{\"foo\":\"Foo\",");
		DataBuffer second = dataBuffer("\"blah\":{\"blah\":123}}");

		testDecodeToMono(Flux.just(first, second), Msg.class, step -> step.verifyError(DecodingException.class));
	}

	private DataBuffer dataBuffer(String json) {
		return this.bufferFactory.wrap(json.getBytes());
	}

}
