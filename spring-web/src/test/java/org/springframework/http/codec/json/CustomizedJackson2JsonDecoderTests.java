/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.json;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;

/**
 * Tests for a customized {@link Jackson2JsonDecoder}.
 *
 * @author Jason Laber
 */
@SuppressWarnings("removal")
class CustomizedJackson2JsonDecoderTests extends AbstractDecoderTests<Jackson2JsonDecoder> {

	CustomizedJackson2JsonDecoderTests() {
		super(new Jackson2JsonDecoderWithCustomization());
	}


	@Override
	public void canDecode() throws Exception {
		// Not Testing, covered under Jackson2JsonDecoderTests
	}

	@Test
	@Override
	protected void decode() throws Exception {
		Flux<DataBuffer> input = Flux.concat(stringBuffer("{\"property\":\"Value1\"}"));

		testDecodeAll(input, MyCustomizedDecoderBean.class, step -> step
				.expectNextMatches(obj -> obj.getProperty() == MyCustomDecoderEnum.VAL1)
				.verifyComplete());
	}

	@Test
	@Override
	protected void decodeToMono() throws Exception {
		Mono<DataBuffer> input = stringBuffer("{\"property\":\"Value2\"}");

		ResolvableType elementType = ResolvableType.forClass(MyCustomizedDecoderBean.class);

		testDecodeToMono(input, elementType, step -> step
				.expectNextMatches(obj -> ((MyCustomizedDecoderBean)obj).getProperty() == MyCustomDecoderEnum.VAL2)
				.expectComplete()
				.verify(), null, null);
	}

	private Mono<DataBuffer> stringBuffer(String value) {
		return stringBuffer(value, StandardCharsets.UTF_8);
	}

	private Mono<DataBuffer> stringBuffer(String value, Charset charset) {
		return Mono.defer(() -> {
			byte[] bytes = value.getBytes(charset);
			DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
			buffer.write(bytes);
			return Mono.just(buffer);
		});
	}


	private static class MyCustomizedDecoderBean {

		private MyCustomDecoderEnum property;

		public MyCustomDecoderEnum getProperty() {
			return property;
		}

		@SuppressWarnings("unused")
		public void setProperty(MyCustomDecoderEnum property) {
			this.property = property;
		}
	}


	private enum MyCustomDecoderEnum {
		VAL1,
		VAL2;

		@Override
		public String toString() {
			return this == VAL1 ? "Value1" : "Value2";
		}
	}


	private static class Jackson2JsonDecoderWithCustomization extends Jackson2JsonDecoder {

		@Override
		protected ObjectReader customizeReader(
				ObjectReader reader, ResolvableType elementType, Map<String, Object> hints) {

			return reader.with(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		}
	}

}
