/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ExponentialBackOff}.
 *
 * @author Stephane Nicoll
 */
class ExponentialBackOffTests {

	@Test
	void defaultInstance() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(3000L);
		assertThat(execution.nextBackOff()).isEqualTo(4500L);
	}

	@Test
	void simpleIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 2.0);
		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(200L);
		assertThat(execution.nextBackOff()).isEqualTo(400L);
		assertThat(execution.nextBackOff()).isEqualTo(800L);
	}

	@Test
	void fixedIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 1.0);
		backOff.setMaxElapsedTime(300L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void maxIntervalReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxInterval(4000L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		// max reached
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
	}

	@Test
	void maxAttemptsReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxElapsedTime(4000L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		// > 4 sec wait in total
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void startReturnDifferentInstances() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(2000L);
		backOff.setMultiplier(2.0);
		backOff.setMaxElapsedTime(4000L);

		BackOffExecution execution = backOff.start();
		BackOffExecution execution2 = backOff.start();

		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution2.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		assertThat(execution2.nextBackOff()).isEqualTo(4000L);
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
		assertThat(execution2.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void invalidInterval() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		assertThatIllegalArgumentException().isThrownBy(() ->
				backOff.setMultiplier(0.9));
	}

	@Test
	void maxIntervalReachedImmediately() {
		ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
		backOff.setMaxInterval(50L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(50L);
		assertThat(execution.nextBackOff()).isEqualTo(50L);
	}

	@Test
	void executionToStringContent() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		BackOffExecution execution = backOff.start();
		assertThat(execution.toString()).isEqualTo("ExponentialBackOffExecution{currentInterval=n/a, multiplier=2.0, attempts=0}");
		execution.nextBackOff();
		assertThat(execution.toString()).isEqualTo("ExponentialBackOffExecution{currentInterval=2000ms, multiplier=2.0, attempts=1}");
		execution.nextBackOff();
		assertThat(execution.toString()).isEqualTo("ExponentialBackOffExecution{currentInterval=4000ms, multiplier=2.0, attempts=2}");
	}

	@Test
	void maxAttempts() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(1000L);
		backOff.setMultiplier(2.0);
		backOff.setMaxInterval(10000L);
		backOff.setMaxAttempts(6);
		List<Long> delays = new ArrayList<>();
		BackOffExecution execution = backOff.start();
		IntStream.range(0, 7).forEach(i -> delays.add(execution.nextBackOff()));
		assertThat(delays).containsExactly(1000L, 2000L, 4000L, 8000L, 10000L, 10000L, BackOffExecution.STOP);
	}

}
