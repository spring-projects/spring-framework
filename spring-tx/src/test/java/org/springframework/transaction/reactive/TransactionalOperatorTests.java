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

package org.springframework.transaction.reactive;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TransactionalOperator}.
 *
 * @author Mark Paluch
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
		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isFalse();
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
		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isFalse();
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

}
