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
package org.springframework.http.codec;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.google.protobuf.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;
import org.springframework.util.MimeType;
import org.springframework.web.testfixture.xml.Pojo;

/**
 * Test scenarios for data buffer leaks.
 * @author Rossen Stoyanchev
 */
public class CancelWithoutDemandCodecTests {

	private final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();


	@AfterEach
	public void tearDown() throws Exception {
		this.bufferFactory.checkForLeaks();
	}


	@Test // gh-22107
	public void cancelWithEncoderHttpMessageWriterAndSingleValue() {
		CharSequenceEncoder encoder = CharSequenceEncoder.allMimeTypes();
		HttpMessageWriter<CharSequence> writer = new EncoderHttpMessageWriter<>(encoder);
		CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

		writer.write(Mono.just("foo"), ResolvableType.forType(String.class), MediaType.TEXT_PLAIN,
				outputMessage, Collections.emptyMap()).block(Duration.ofSeconds(5));
	}

	@Test // gh-22107
	public void cancelWithJackson() {
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();

		Flux<DataBuffer> flux = encoder.encode(Flux.just(new Pojo("foofoo", "barbar"), new Pojo("bar", "baz")),
				this.bufferFactory, ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_JSON, Collections.emptyMap());

		BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
		flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
		subscriber.cancel();
	}

	@Test // gh-22107
	public void cancelWithJaxb2() {
		Jaxb2XmlEncoder encoder = new Jaxb2XmlEncoder();

		Flux<DataBuffer> flux = encoder.encode(Mono.just(new Pojo("foo", "bar")),
				this.bufferFactory, ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML, Collections.emptyMap());

		BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
		flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
		subscriber.cancel();
	}

	@Test // gh-22543
	public void cancelWithProtobufEncoder() {
		ProtobufEncoder encoder = new ProtobufEncoder();
		Msg msg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

		Flux<DataBuffer> flux = encoder.encode(Mono.just(msg),
				this.bufferFactory, ResolvableType.forClass(Msg.class),
				new MimeType("application", "x-protobuf"), Collections.emptyMap());

		BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
		flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
		subscriber.cancel();
	}

	@Test // gh-22731
	public void cancelWithProtobufDecoder() throws InterruptedException {
		ProtobufDecoder decoder = new ProtobufDecoder();

		Mono<DataBuffer> input = Mono.fromCallable(() -> {
			Msg msg = Msg.newBuilder().setFoo("Foo").build();
			byte[] bytes = msg.toByteArray();
			DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
			buffer.write(bytes);
			return buffer;
		});

		Flux<Message> messages = decoder.decode(input, ResolvableType.forType(Msg.class),
				new MimeType("application", "x-protobuf"), Collections.emptyMap());
		ZeroDemandMessageSubscriber subscriber = new ZeroDemandMessageSubscriber();
		messages.subscribe(subscriber);
		subscriber.cancel();
	}

	@Test // gh-22107
	public void cancelWithMultipartContent() {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("part1", "value1");
		builder.part("part2", "value2");

		List<HttpMessageWriter<?>> writers = ClientCodecConfigurer.create().getWriters();
		MultipartHttpMessageWriter writer = new MultipartHttpMessageWriter(writers);
		CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

		writer.write(Mono.just(builder.build()), null, MediaType.MULTIPART_FORM_DATA,
				outputMessage, Collections.emptyMap()).block(Duration.ofSeconds(5));
	}

	@Test // gh-22107
	public void cancelWithSse() {
		ServerSentEvent<?> event = ServerSentEvent.builder().data("bar").id("c42").event("foo").build();
		ServerSentEventHttpMessageWriter writer = new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder());
		CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

		writer.write(Mono.just(event), ResolvableType.forClass(ServerSentEvent.class), MediaType.TEXT_EVENT_STREAM,
				outputMessage, Collections.emptyMap()).block(Duration.ofSeconds(5));
	}



	private static class CancellingOutputMessage implements ReactiveHttpOutputMessage {

		private final DataBufferFactory bufferFactory;


		public CancellingOutputMessage(DataBufferFactory bufferFactory) {
			this.bufferFactory = bufferFactory;
		}


		@Override
		public DataBufferFactory bufferFactory() {
			return this.bufferFactory;
		}

		@Override
		public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		}

		@Override
		public boolean isCommitted() {
			return false;
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			Flux<? extends DataBuffer> flux = Flux.from(body);
			BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
			flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
			subscriber.cancel();
			return Mono.empty();
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			Flux<? extends DataBuffer> flux = Flux.from(body).concatMap(Flux::from);
			BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
			flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
			subscriber.cancel();
			return Mono.empty();
		}

		@Override
		public Mono<Void> setComplete() {
			throw new UnsupportedOperationException();
		}

		@Override
		public HttpHeaders getHeaders() {
			return new HttpHeaders();
		}
	}


	private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			// Just subscribe without requesting
		}
	}


	private static class ZeroDemandMessageSubscriber extends BaseSubscriber<Message> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			// Just subscribe without requesting
		}
	}
}
