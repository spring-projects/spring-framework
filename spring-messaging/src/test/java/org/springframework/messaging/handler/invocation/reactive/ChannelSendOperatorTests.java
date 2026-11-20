/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation.reactive;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChannelSendOperator}.
 *
 * <p>See also {@code ChannelSendOperatorTests} in {@code spring-web}.
 */
class ChannelSendOperatorTests {

	@Test  // gh-37527
	void sourceCancelledWhenWriteFunctionResultErrorsWhileItemCached() {
		AtomicReference<SignalType> sourceSignal = new AtomicReference<>();
		Flux<String> source = Flux.just("one").concatWith(Flux.never()).doFinally(sourceSignal::set);

		ChannelSendOperator<String> operator = new ChannelSendOperator<>(source,
				publisher -> Mono.error(new IllegalStateException("boom")));

		StepVerifier.create(operator).expectErrorMessage("boom").verify(Duration.ofSeconds(5));
		assertThat(sourceSignal.get()).isEqualTo(SignalType.CANCEL);
	}

	@Test  // gh-37527
	void sourceCancelledWhenWriteFunctionThrowsWhileItemCached() {
		AtomicReference<SignalType> sourceSignal = new AtomicReference<>();
		Flux<String> source = Flux.just("one").concatWith(Flux.never()).doFinally(sourceSignal::set);

		ChannelSendOperator<String> operator = new ChannelSendOperator<>(source, publisher -> {
			throw new IllegalStateException("boom");
		});

		StepVerifier.create(operator).expectErrorMessage("boom").verify(Duration.ofSeconds(5));
		assertThat(sourceSignal.get()).isEqualTo(SignalType.CANCEL);
	}

	@Test  // gh-37527
	void sourceCancelledAndCachedItemReleasedWhenWriteFunctionResultErrors() {
		LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();
		AtomicReference<SignalType> sourceSignal = new AtomicReference<>();

		ChannelSendOperator<DataBuffer> operator = new ChannelSendOperator<>(
				Flux.<DataBuffer>create(sink -> {
					DataBuffer dataBuffer = bufferFactory.allocateBuffer(256);
					dataBuffer.write("foo", StandardCharsets.UTF_8);
					sink.next(dataBuffer);
				}).doFinally(sourceSignal::set),
				publisher -> Mono.error(new IllegalStateException("boom")));

		StepVerifier.create(operator).expectErrorMessage("boom").verify(Duration.ofSeconds(5));
		assertThat(sourceSignal.get()).isEqualTo(SignalType.CANCEL);
		bufferFactory.checkForLeaks();
	}

}
