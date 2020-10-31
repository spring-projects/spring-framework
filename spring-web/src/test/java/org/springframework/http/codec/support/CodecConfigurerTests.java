/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec.support;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.protobuf.ExtensionRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.NettyByteBufDecoder;
import org.springframework.core.codec.NettyByteBufEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BaseDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class CodecConfigurerTests {

	private final CodecConfigurer configurer = new TestCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger();


	@Test
	void defaultReaders() {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(readers.size()).isEqualTo(12);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(NettyByteBufDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
		assertStringDecoder(getNextDecoder(readers), true);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jaxb2XmlDecoder.class);
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	void defaultWriters() {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertThat(writers.size()).isEqualTo(11);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(NettyByteBufEncoder.class);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
		assertStringEncoder(getNextEncoder(writers), true);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jaxb2XmlEncoder.class);
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	void defaultAndCustomReaders() {
		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		given(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		given(customReader1.canRead(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customReader2.canRead(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().register(customDecoder1);
		this.configurer.customCodecs().register(customDecoder2);

		this.configurer.customCodecs().register(customReader1);
		this.configurer.customCodecs().register(customReader2);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertThat(readers.size()).isEqualTo(16);
		assertThat(getNextDecoder(readers)).isSameAs(customDecoder1);
		assertThat(readers.get(this.index.getAndIncrement())).isSameAs(customReader1);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(NettyByteBufDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(StringDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
		assertThat(getNextDecoder(readers)).isSameAs(customDecoder2);
		assertThat(readers.get(this.index.getAndIncrement())).isSameAs(customReader2);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jaxb2XmlDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(StringDecoder.class);
	}

	@Test
	void defaultAndCustomWriters() {
		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		given(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		given(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().register(customEncoder1);
		this.configurer.customCodecs().register(customEncoder2);

		this.configurer.customCodecs().register(customWriter1);
		this.configurer.customCodecs().register(customWriter2);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertThat(writers.size()).isEqualTo(15);
		assertThat(getNextEncoder(writers)).isSameAs(customEncoder1);
		assertThat(writers.get(this.index.getAndIncrement())).isSameAs(customWriter1);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(NettyByteBufEncoder.class);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(CharSequenceEncoder.class);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
		assertThat(getNextEncoder(writers)).isSameAs(customEncoder2);
		assertThat(writers.get(this.index.getAndIncrement())).isSameAs(customWriter2);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jaxb2XmlEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(CharSequenceEncoder.class);
	}

	@Test
	void defaultsOffCustomReaders() {
		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		given(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		given(customReader1.canRead(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customReader2.canRead(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().register(customDecoder1);
		this.configurer.customCodecs().register(customDecoder2);

		this.configurer.customCodecs().register(customReader1);
		this.configurer.customCodecs().register(customReader2);

		this.configurer.registerDefaults(false);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertThat(readers.size()).isEqualTo(4);
		assertThat(getNextDecoder(readers)).isSameAs(customDecoder1);
		assertThat(readers.get(this.index.getAndIncrement())).isSameAs(customReader1);
		assertThat(getNextDecoder(readers)).isSameAs(customDecoder2);
		assertThat(readers.get(this.index.getAndIncrement())).isSameAs(customReader2);
	}

	@Test
	void defaultsOffWithCustomWriters() {
		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		given(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).willReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		given(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(false);
		given(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).willReturn(true);

		this.configurer.customCodecs().register(customEncoder1);
		this.configurer.customCodecs().register(customEncoder2);

		this.configurer.customCodecs().register(customWriter1);
		this.configurer.customCodecs().register(customWriter2);

		this.configurer.registerDefaults(false);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertThat(writers.size()).isEqualTo(4);
		assertThat(getNextEncoder(writers)).isSameAs(customEncoder1);
		assertThat(writers.get(this.index.getAndIncrement())).isSameAs(customWriter1);
		assertThat(getNextEncoder(writers)).isSameAs(customEncoder2);
		assertThat(writers.get(this.index.getAndIncrement())).isSameAs(customWriter2);
	}

	@Test
	void encoderDecoderOverrides() {
		Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder();
		Jackson2JsonEncoder jacksonEncoder = new Jackson2JsonEncoder();
		Jackson2SmileDecoder smileDecoder = new Jackson2SmileDecoder();
		Jackson2SmileEncoder smileEncoder = new Jackson2SmileEncoder();
		ProtobufDecoder protobufDecoder = new ProtobufDecoder(ExtensionRegistry.newInstance());
		ProtobufEncoder protobufEncoder = new ProtobufEncoder();
		Jaxb2XmlEncoder jaxb2Encoder = new Jaxb2XmlEncoder();
		Jaxb2XmlDecoder jaxb2Decoder = new Jaxb2XmlDecoder();

		this.configurer.defaultCodecs().jackson2JsonDecoder(jacksonDecoder);
		this.configurer.defaultCodecs().jackson2JsonEncoder(jacksonEncoder);
		this.configurer.defaultCodecs().jackson2SmileDecoder(smileDecoder);
		this.configurer.defaultCodecs().jackson2SmileEncoder(smileEncoder);
		this.configurer.defaultCodecs().protobufDecoder(protobufDecoder);
		this.configurer.defaultCodecs().protobufEncoder(protobufEncoder);
		this.configurer.defaultCodecs().jaxb2Decoder(jaxb2Decoder);
		this.configurer.defaultCodecs().jaxb2Encoder(jaxb2Encoder);

		assertDecoderInstance(jacksonDecoder);
		assertDecoderInstance(smileDecoder);
		assertDecoderInstance(protobufDecoder);
		assertDecoderInstance(jaxb2Decoder);
		assertEncoderInstance(jacksonEncoder);
		assertEncoderInstance(smileEncoder);
		assertEncoderInstance(protobufEncoder);
		assertEncoderInstance(jaxb2Encoder);
	}

	@Test
	void cloneEmptyCustomCodecs() {
		this.configurer.registerDefaults(false);
		assertThat(this.configurer.getReaders()).isEmpty();
		assertThat(this.configurer.getWriters()).isEmpty();

		CodecConfigurer clone = this.configurer.clone();
		clone.customCodecs().register(new Jackson2JsonEncoder());
		clone.customCodecs().register(new Jackson2JsonDecoder());
		clone.customCodecs().register(new ServerSentEventHttpMessageReader());
		clone.customCodecs().register(new ServerSentEventHttpMessageWriter());

		assertThat(this.configurer.getReaders()).isEmpty();
		assertThat(this.configurer.getWriters()).isEmpty();
		assertThat(clone.getReaders()).hasSize(2);
		assertThat(clone.getWriters()).hasSize(2);
	}

	@Test
	void cloneCustomCodecs() {
		this.configurer.registerDefaults(false);
		assertThat(this.configurer.getReaders()).isEmpty();
		assertThat(this.configurer.getWriters()).isEmpty();

		this.configurer.customCodecs().register(new Jackson2JsonEncoder());
		this.configurer.customCodecs().register(new Jackson2JsonDecoder());
		this.configurer.customCodecs().register(new ServerSentEventHttpMessageReader());
		this.configurer.customCodecs().register(new ServerSentEventHttpMessageWriter());
		assertThat(this.configurer.getReaders()).hasSize(2);
		assertThat(this.configurer.getWriters()).hasSize(2);

		CodecConfigurer clone = this.configurer.clone();
		assertThat(this.configurer.getReaders()).hasSize(2);
		assertThat(this.configurer.getWriters()).hasSize(2);
		assertThat(clone.getReaders()).hasSize(2);
		assertThat(clone.getWriters()).hasSize(2);
	}

	@Test
	void cloneDefaultCodecs() {
		CodecConfigurer clone = this.configurer.clone();

		Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder();
		Jackson2JsonEncoder jacksonEncoder = new Jackson2JsonEncoder();
		Jaxb2XmlDecoder jaxb2Decoder = new Jaxb2XmlDecoder();
		Jaxb2XmlEncoder jaxb2Encoder = new Jaxb2XmlEncoder();
		ProtobufDecoder protoDecoder = new ProtobufDecoder();
		ProtobufEncoder protoEncoder = new ProtobufEncoder();

		clone.defaultCodecs().jackson2JsonDecoder(jacksonDecoder);
		clone.defaultCodecs().jackson2JsonEncoder(jacksonEncoder);
		clone.defaultCodecs().jaxb2Decoder(jaxb2Decoder);
		clone.defaultCodecs().jaxb2Encoder(jaxb2Encoder);
		clone.defaultCodecs().protobufDecoder(protoDecoder);
		clone.defaultCodecs().protobufEncoder(protoEncoder);

		// Clone has the customized the customizations

		List<Decoder<?>> decoders = clone.getReaders().stream()
				.filter(reader -> reader instanceof DecoderHttpMessageReader)
				.map(reader -> ((DecoderHttpMessageReader<?>) reader).getDecoder())
				.collect(Collectors.toList());

		List<Encoder<?>> encoders = clone.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(reader -> ((EncoderHttpMessageWriter<?>) reader).getEncoder())
				.collect(Collectors.toList());

		assertThat(decoders).contains(jacksonDecoder, jaxb2Decoder, protoDecoder);
		assertThat(encoders).contains(jacksonEncoder, jaxb2Encoder, protoEncoder);

		// Original does not have the customizations

		decoders = this.configurer.getReaders().stream()
				.filter(reader -> reader instanceof DecoderHttpMessageReader)
				.map(reader -> ((DecoderHttpMessageReader<?>) reader).getDecoder())
				.collect(Collectors.toList());

		encoders = this.configurer.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(reader -> ((EncoderHttpMessageWriter<?>) reader).getEncoder())
				.collect(Collectors.toList());

		assertThat(decoders).doesNotContain(jacksonDecoder, jaxb2Decoder, protoDecoder);
		assertThat(encoders).doesNotContain(jacksonEncoder, jaxb2Encoder, protoEncoder);
	}

	@SuppressWarnings("deprecation")
	@Test
	void withDefaultCodecConfig() {
		AtomicBoolean callbackCalled = new AtomicBoolean();
		this.configurer.defaultCodecs().enableLoggingRequestDetails(true);
		this.configurer.customCodecs().withDefaultCodecConfig(config -> {
			assertThat(config.isEnableLoggingRequestDetails()).isTrue();
			callbackCalled.compareAndSet(false, true);
		});
		this.configurer.getReaders();
		assertThat(callbackCalled).isTrue();
	}

	private Decoder<?> getNextDecoder(List<HttpMessageReader<?>> readers) {
		HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
		assertThat(reader.getClass()).isEqualTo(DecoderHttpMessageReader.class);
		return ((DecoderHttpMessageReader<?>) reader).getDecoder();
	}

	private Encoder<?> getNextEncoder(List<HttpMessageWriter<?>> writers) {
		HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertThat(writer.getClass()).isEqualTo(EncoderHttpMessageWriter.class);
		return ((EncoderHttpMessageWriter<?>) writer).getEncoder();
	}

	private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
		assertThat(decoder.getClass()).isEqualTo(StringDecoder.class);
		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertThat(encoder.getClass()).isEqualTo(CharSequenceEncoder.class);
		assertThat(encoder.canEncode(ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat(encoder.canEncode(ResolvableType.forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
	}

	private void assertDecoderInstance(Decoder<?> decoder) {
		assertThat(this.configurer.getReaders().stream()
				.filter(writer -> writer instanceof DecoderHttpMessageReader)
				.map(writer -> ((DecoderHttpMessageReader<?>) writer).getDecoder())
				.filter(e -> decoder.getClass().equals(e.getClass()))
				.findFirst()
				.filter(e -> e == decoder).orElse(null)).isSameAs(decoder);
	}

	private void assertEncoderInstance(Encoder<?> encoder) {
		assertThat(this.configurer.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(writer -> ((EncoderHttpMessageWriter<?>) writer).getEncoder())
				.filter(e -> encoder.getClass().equals(e.getClass()))
				.findFirst()
				.filter(e -> e == encoder).orElse(null)).isSameAs(encoder);
	}


	private static class TestCodecConfigurer extends BaseCodecConfigurer {

		TestCodecConfigurer() {
			super(new BaseDefaultCodecs());
		}

		TestCodecConfigurer(TestCodecConfigurer other) {
			super(other);
		}

		@Override
		protected BaseDefaultCodecs cloneDefaultCodecs() {
			return new BaseDefaultCodecs((BaseDefaultCodecs) defaultCodecs());
		}

		@Override
		public CodecConfigurer clone() {
			return new TestCodecConfigurer(this);
		}
	}

}
