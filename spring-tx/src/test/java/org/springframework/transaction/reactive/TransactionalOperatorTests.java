/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.transaction.reactive;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;

import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionalOperator}.
 *
 * @author Mark Paluch
 * @author Enric Sala
 */
public class TransactionalOperatorTests {

	ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);


	@Test
	public void commitWithMono() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Mono.just(true).as(operator::transactional)
				.as(StepVerifier::create)
				.expectNext(true)
				.verifyComplete();
		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isFalse();
	}

	@Test
	public void monoSubscriptionNotCancelled() {
		AtomicBoolean cancelled = new AtomicBoolean();
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Mono.just(true).doOnCancel(() -> cancelled.set(true)).as(operator::transactional)
				.as(StepVerifier::create)
				.expectNext(true)
				.verifyComplete();
		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isFalse();
		assertThat(cancelled).isFalse();
	}

	@Test
	public void cancellationPropagatedToMono() {
		AtomicBoolean cancelled = new AtomicBoolean();
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Mono.create(sink -> sink.onCancel(() -> cancelled.set(true))).as(operator::transactional)
				.as(StepVerifier::create)
				.thenAwait()
				.thenCancel()
				.verify();
		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isTrue();
		assertThat(cancelled).isTrue();
	}

	@Test
	public void cancellationPropagatedToFlux() {
		AtomicBoolean cancelled = new AtomicBoolean();
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Flux.create(sink -> sink.onCancel(() -> cancelled.set(true))).as(operator::transactional)
				.as(StepVerifier::create)
				.thenAwait()
				.thenCancel()
				.verify();
		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isTrue();
		assertThat(cancelled).isTrue();
	}

	@Test
	public void rollbackWithMono() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Mono.error(new IllegalStateException()).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyError(IllegalStateException.class);
		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isTrue();
	}

	@Test
	public void commitFailureWithMono() {
		ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
		given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
		PublisherProbe<Void> commit = PublisherProbe.of(Mono.error(IOException::new));
		given(tm.commit(any())).willReturn(commit.mono());
		PublisherProbe<Void> rollback = PublisherProbe.empty();
		given(tm.rollback(any())).willReturn(rollback.mono());

		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Mono.just(true).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyError(IOException.class);
		assertThat(commit.subscribeCount()).isEqualTo(1);
		rollback.assertWasNotSubscribed();
	}

	@Test
	public void rollbackFailureWithMono() {
		ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
		given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
		PublisherProbe<Void> commit = PublisherProbe.empty();
		given(tm.commit(any())).willReturn(commit.mono());
		PublisherProbe<Void> rollback = PublisherProbe.of(Mono.error(IOException::new));
		given(tm.rollback(any())).willReturn(rollback.mono());

		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		IllegalStateException actionFailure = new IllegalStateException();
		Mono.error(actionFailure).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex)
						.isInstanceOf(IOException.class)
						.hasSuppressedException(actionFailure));
		commit.assertWasNotSubscribed();
		assertThat(rollback.subscribeCount()).isEqualTo(1);
	}

	@Test
	public void commitWithFlux() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Flux.just(1, 2, 3, 4).as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(4)
				.verifyComplete();
		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isFalse();
	}

	@Test
	public void rollbackWithFlux() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Flux.error(new IllegalStateException()).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyError(IllegalStateException.class);
		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isTrue();
	}

	@Test
	public void commitFailureWithFlux() {
		ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
		given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
		PublisherProbe<Void> commit = PublisherProbe.of(Mono.error(IOException::new));
		given(tm.commit(any())).willReturn(commit.mono());
		PublisherProbe<Void> rollback = PublisherProbe.empty();
		given(tm.rollback(any())).willReturn(rollback.mono());

		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Flux.just(1, 2, 3, 4).as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(4)
				.verifyError(IOException.class);
		assertThat(commit.subscribeCount()).isEqualTo(1);
		rollback.assertWasNotSubscribed();
	}

	@Test
	public void rollbackFailureWithFlux() {
		ReactiveTransactionManager tm = mock(ReactiveTransactionManager.class);
		given(tm.getReactiveTransaction(any())).willReturn(Mono.just(mock(ReactiveTransaction.class)));
		PublisherProbe<Void> commit = PublisherProbe.empty();
		given(tm.commit(any())).willReturn(commit.mono());
		PublisherProbe<Void> rollback = PublisherProbe.of(Mono.error(IOException::new));
		given(tm.rollback(any())).willReturn(rollback.mono());

		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		IllegalStateException actionFailure = new IllegalStateException();
		Flux.just(1, 2, 3).concatWith(Flux.error(actionFailure)).as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(3)
				.verifyErrorSatisfies(ex -> assertThat(ex)
						.isInstanceOf(IOException.class)
						.hasSuppressedException(actionFailure));
		commit.assertWasNotSubscribed();
		assertThat(rollback.subscribeCount()).isEqualTo(1);
	}

}
