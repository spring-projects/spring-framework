/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.rsocket;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;
import io.rsocket.AbstractRSocket;
import io.rsocket.Frame;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.*;

/**
 * Tests for scenarios that could lead to Payload and/or DataBuffer leaks.
 *
 * @author Rossen Stoyanchev
 */
public class RSocketBufferLeakTests {

	private static AnnotationConfigApplicationContext context;

	private static final PayloadInterceptor payloadInterceptor = new PayloadInterceptor();

	private static CloseableChannel server;

	private static RSocket client;

	private static RSocketRequester requester;


	@BeforeClass
	@SuppressWarnings("ConstantConditions")
	public static void setupOnce() {
		context = new AnnotationConfigApplicationContext(ServerConfig.class);

		server = RSocketFactory.receive()
				.frameDecoder(Frame::retain) // zero copy
				.addServerPlugin(payloadInterceptor) // intercept responding
				.acceptor(context.getBean(MessageHandlerAcceptor.class))
				.transport(TcpServerTransport.create("localhost", 7000))
				.start()
				.block();

		client = RSocketFactory.connect()
				.frameDecoder(Frame::retain) // zero copy
				.addClientPlugin(payloadInterceptor) // intercept outgoing requests
				.dataMimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
				.transport(TcpClientTransport.create("localhost", 7000))
				.start()
				.block();

		requester = RSocketRequester.create(
				client, MimeTypeUtils.TEXT_PLAIN, context.getBean(RSocketStrategies.class));
	}

	@AfterClass
	public static void tearDownOnce() {
		client.dispose();
		server.dispose();
	}


	@Before
	public void setUp() {
		getLeakAwareNettyDataBufferFactory().reset();
		payloadInterceptor.reset();
	}

	@After
	public void tearDown() throws InterruptedException {
		getLeakAwareNettyDataBufferFactory().checkForLeaks(Duration.ofSeconds(5));
		payloadInterceptor.checkForLeaks();
	}

	private LeakAwareNettyDataBufferFactory getLeakAwareNettyDataBufferFactory() {
		return (LeakAwareNettyDataBufferFactory) context.getBean(RSocketStrategies.class).dataBufferFactory();
	}


	@Test
	public void assemblyTimeErrorForHandleAndReply() {
		Mono<String> result = requester.route("A.B").data("foo").retrieveMono(String.class);
		StepVerifier.create(result).expectErrorMatches(ex -> {
			String prefix = "Ambiguous handler methods mapped for destination 'A.B':";
			return ex.getMessage().startsWith(prefix);
		}).verify();
	}

	@Test
	public void subscriptionTimeErrorForHandleAndReply() {
		Mono<String> result = requester.route("not-decodable").data("foo").retrieveMono(String.class);
		StepVerifier.create(result).expectErrorMatches(ex -> {
			String prefix = "Cannot decode to [org.springframework.core.io.Resource]";
			return ex.getMessage().contains(prefix);
		}).verify();
	}

	@Test
	public void errorSignalWithExceptionHandler() {
		Mono<String> result = requester.route("error-signal").data("foo").retrieveMono(String.class);
		StepVerifier.create(result).expectNext("Handled 'bad input'").verifyComplete();
	}

	@Test
	public void ignoreInput() {
		Flux<String> result = requester.route("ignore-input").data("a").retrieveFlux(String.class);
		StepVerifier.create(result).expectNext("bar").verifyComplete();
	}

	@Test
	public void retrieveMonoFromFluxResponderMethod() {
		Mono<String> result = requester.route("request-stream").data("foo").retrieveMono(String.class);
		StepVerifier.create(result).expectNext("foo-1").verifyComplete();
	}


	@Controller
	static class ServerController {

		@MessageMapping("A.*")
		void ambiguousMatchA(String payload) {
			throw new IllegalStateException("Unexpected call");
		}

		@MessageMapping("*.B")
		void ambiguousMatchB(String payload) {
			throw new IllegalStateException("Unexpected call");
		}

		@MessageMapping("not-decodable")
		void notDecodable(@Payload Resource resource) {
			throw new IllegalStateException("Unexpected call");
		}

		@MessageMapping("error-signal")
		public Flux<String> errorSignal(String payload) {
			return Flux.error(new IllegalArgumentException("bad input"))
					.delayElements(Duration.ofMillis(10))
					.cast(String.class);
		}

		@MessageExceptionHandler
		public String handleIllegalArgument(IllegalArgumentException ex) {
			return "Handled '" + ex.getMessage() + "'";
		}

		@MessageMapping("ignore-input")
		Mono<String> ignoreInput() {
			return Mono.delay(Duration.ofMillis(10)).map(l -> "bar");
		}

		@MessageMapping("request-stream")
		Flux<String> stream(String payload) {
			return Flux.range(1,100).delayElements(Duration.ofMillis(10)).map(idx -> payload + "-" + idx);
		}
	}


	@Configuration
	static class ServerConfig {

		@Bean
		public ServerController controller() {
			return new ServerController();
		}

		@Bean
		public MessageHandlerAcceptor messageHandlerAcceptor() {
			MessageHandlerAcceptor acceptor = new MessageHandlerAcceptor();
			acceptor.setRSocketStrategies(rsocketStrategies());
			return acceptor;
		}

		@Bean
		public RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.builder()
					.decoder(StringDecoder.allMimeTypes())
					.encoder(CharSequenceEncoder.allMimeTypes())
					.dataBufferFactory(new LeakAwareNettyDataBufferFactory(PooledByteBufAllocator.DEFAULT))
					.build();
		}
	}


	/**
	 * Similar {@link org.springframework.core.io.buffer.LeakAwareDataBufferFactory}
	 * but extends {@link NettyDataBufferFactory} rather than rely on
	 * decoration, since {@link PayloadUtils} does instanceof checks.
	 */
	private static class LeakAwareNettyDataBufferFactory extends NettyDataBufferFactory {

		private final List<DataBufferLeakInfo> created = new ArrayList<>();

		LeakAwareNettyDataBufferFactory(ByteBufAllocator byteBufAllocator) {
			super(byteBufAllocator);
		}

		void checkForLeaks(Duration duration) throws InterruptedException {
			Instant start = Instant.now();
			while (true) {
				try {
					this.created.forEach(info -> {
						if (((PooledDataBuffer) info.getDataBuffer()).isAllocated()) {
							throw info.getError();
						}
					});
					break;
				}
				catch (AssertionError ex) {
					if (Instant.now().isAfter(start.plus(duration))) {
						throw ex;
					}
				}
				Thread.sleep(50);
			}
		}

		void reset() {
			this.created.clear();
		}


		@Override
		public NettyDataBuffer allocateBuffer() {
			return (NettyDataBuffer) record(super.allocateBuffer());
		}

		@Override
		public NettyDataBuffer allocateBuffer(int initialCapacity) {
			return (NettyDataBuffer) record(super.allocateBuffer(initialCapacity));
		}

		@Override
		public NettyDataBuffer wrap(ByteBuf byteBuf) {
			NettyDataBuffer dataBuffer = super.wrap(byteBuf);
			if (byteBuf != Unpooled.EMPTY_BUFFER) {
				record(dataBuffer);
			}
			return dataBuffer;
		}

		@Override
		public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
			return record(super.join(dataBuffers));
		}

		private DataBuffer record(DataBuffer buffer) {
			this.created.add(new DataBufferLeakInfo(buffer, new AssertionError(String.format(
					"DataBuffer leak: {%s} {%s} not released.%nStacktrace at buffer creation: ", buffer,
					ObjectUtils.getIdentityHexString(((NettyDataBuffer) buffer).getNativeBuffer())))));
			return buffer;
		}
	}


	private static class DataBufferLeakInfo {

		private final DataBuffer dataBuffer;

		private final AssertionError error;

		DataBufferLeakInfo(DataBuffer dataBuffer, AssertionError error) {
			this.dataBuffer = dataBuffer;
			this.error = error;
		}

		DataBuffer getDataBuffer() {
			return this.dataBuffer;
		}

		AssertionError getError() {
			return this.error;
		}
	}


	/**
	 * Store all intercepted incoming and outgoing payloads and then use
	 * {@link #checkForLeaks()} at the end to check reference counts.
	 */
	private static class PayloadInterceptor extends AbstractRSocket implements RSocketInterceptor {

		private final List<PayloadSavingDecorator> rsockets = new CopyOnWriteArrayList<>();

		void checkForLeaks() {
			this.rsockets.stream().map(PayloadSavingDecorator::getPayloads)
					.forEach(payloadInfoProcessor -> {
						payloadInfoProcessor.onComplete();
						payloadInfoProcessor
								.doOnNext(this::checkForLeak)
								.blockLast();
					});
		}

		private void checkForLeak(PayloadLeakInfo info) {
			Instant start = Instant.now();
			while (true) {
				try {
					int count = info.getReferenceCount();
					assertTrue("Leaked payload (refCnt=" + count + "): " + info, count == 0);
					break;
				}
				catch (AssertionError ex) {
					if (Instant.now().isAfter(start.plus(Duration.ofSeconds(5)))) {
						throw ex;
					}
				}
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException ex) {
					// ignore
				}
			}
		}

		public void reset() {
			this.rsockets.forEach(PayloadSavingDecorator::reset);
		}

		@Override
		public RSocket apply(RSocket rsocket) {
			PayloadSavingDecorator decorator = new PayloadSavingDecorator(rsocket);
			this.rsockets.add(decorator);
			return decorator;
		}


		private static class PayloadSavingDecorator extends AbstractRSocket {

			private final RSocket delegate;

			private ReplayProcessor<PayloadLeakInfo> payloads = ReplayProcessor.create();

			PayloadSavingDecorator(RSocket delegate) {
				this.delegate = delegate;
			}

			ReplayProcessor<PayloadLeakInfo> getPayloads() {
				return this.payloads;
			}

			void reset() {
				this.payloads = ReplayProcessor.create();
			}

			@Override
			public Mono<Void> fireAndForget(io.rsocket.Payload payload) {
				return this.delegate.fireAndForget(addPayload(payload));
			}

			@Override
			public Mono<io.rsocket.Payload> requestResponse(io.rsocket.Payload payload) {
				return this.delegate.requestResponse(addPayload(payload)).doOnSuccess(this::addPayload);
			}

			@Override
			public Flux<io.rsocket.Payload> requestStream(io.rsocket.Payload payload) {
				return this.delegate.requestStream(addPayload(payload)).doOnNext(this::addPayload);
			}

			@Override
			public Flux<io.rsocket.Payload> requestChannel(Publisher<io.rsocket.Payload> payloads) {
				return this.delegate
						.requestChannel(Flux.from(payloads).doOnNext(this::addPayload))
						.doOnNext(this::addPayload);
			}

			private io.rsocket.Payload addPayload(io.rsocket.Payload payload) {
				this.payloads.onNext(new PayloadLeakInfo(payload));
				return payload;
			}

			@Override
			public Mono<Void> metadataPush(io.rsocket.Payload payload) {
				return this.delegate.metadataPush(addPayload(payload));
			}
		}
	}


	private static class PayloadLeakInfo {

		private final String description;

		private final ReferenceCounted referenceCounted;

		PayloadLeakInfo(io.rsocket.Payload payload) {
			this.description = payload.toString();
			this.referenceCounted = payload;
		}

		int getReferenceCount() {
			return this.referenceCounted.refCnt();
		}

		@Override
		public String toString() {
			return this.description;
		}
	}

}
