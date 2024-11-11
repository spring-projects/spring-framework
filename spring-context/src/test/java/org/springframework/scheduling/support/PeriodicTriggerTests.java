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

package org.springframework.scheduling.support;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.NumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @since 3.0
 */
class PeriodicTriggerTests {

	@Test
	void fixedDelayFirstExecution() {
		Instant now = Instant.now();
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(5000));
		Instant next = trigger.nextExecution(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	@SuppressWarnings("deprecation")
	void fixedDelayWithInitialDelayFirstExecution() {
		Instant now = Instant.now();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
		trigger.setInitialDelay(initialDelay);
		Instant next = trigger.nextExecution(context(null, null, null));
		assertApproximateDifference(now, next, initialDelay);
	}

	@Test
	void fixedDelayWithTimeUnitFirstExecution() {
		Instant now = Instant.now();
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(5));
		Instant next = trigger.nextExecution(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	void fixedDelayWithTimeUnitAndInitialDelayFirstExecution() {
		Instant now = Instant.now();
		long period = 5;
		long initialDelay = 30;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(period));
		trigger.setInitialDelay(Duration.ofSeconds(initialDelay));
		Instant next = trigger.nextExecution(context(null, null, null));
		assertApproximateDifference(now, next, initialDelay * 1000);
	}

	@Test
	void fixedDelaySubsequentExecution() {
		Instant now = Instant.now();
		long period = 5000;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
		Instant next = trigger.nextExecution(context(now, 500, 3000));
		assertApproximateDifference(now, next, period + 3000);
	}

	@Test
	@SuppressWarnings("deprecation")
	void fixedDelayWithInitialDelaySubsequentExecution() {
		Instant now = Instant.now();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
		trigger.setInitialDelay(initialDelay);
		Instant next = trigger.nextExecution(context(now, 500, 3000));
		assertApproximateDifference(now, next, period + 3000);
	}

	@Test
	void fixedDelayWithTimeUnitSubsequentExecution() {
		Instant now = Instant.now();
		long period = 5;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(period));
		Instant next = trigger.nextExecution(context(now, 500, 3000));
		assertApproximateDifference(now, next, (period * 1000) + 3000);
	}

	@Test
	void fixedRateFirstExecution() {
		Instant now = Instant.now();
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(5000));
		trigger.setFixedRate(true);
		Instant next = trigger.nextExecution(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	void fixedRateWithTimeUnitFirstExecution() {
		Instant now = Instant.now();
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(5));
		trigger.setFixedRate(true);
		Instant next = trigger.nextExecution(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	@SuppressWarnings("deprecation")
	void fixedRateWithInitialDelayFirstExecution() {
		Instant now = Instant.now();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
		trigger.setFixedRate(true);
		trigger.setInitialDelay(initialDelay);
		Instant next = trigger.nextExecution(context(null, null, null));
		assertApproximateDifference(now, next, initialDelay);
	}

	@Test
	void fixedRateWithTimeUnitAndInitialDelayFirstExecution() {
		Instant now = Instant.now();
		long period = 5;
		long initialDelay = 30;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMinutes(period));
		trigger.setFixedRate(true);
		trigger.setInitialDelay(Duration.ofMinutes(initialDelay));
		Instant next = trigger.nextExecution(context(null, null, null));
		assertApproximateDifference(now, next, (initialDelay * 60 * 1000));
	}

	@Test
	void fixedRateSubsequentExecution() {
		Instant now = Instant.now();
		long period = 5000;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
		trigger.setFixedRate(true);
		Instant next = trigger.nextExecution(context(now, 500, 3000));
		assertApproximateDifference(now, next, period);
	}

	@Test
	@SuppressWarnings("deprecation")
	void fixedRateWithInitialDelaySubsequentExecution() {
		Instant now = Instant.now();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
		trigger.setFixedRate(true);
		trigger.setInitialDelay(initialDelay);
		Instant next = trigger.nextExecution(context(now, 500, 3000));
		assertApproximateDifference(now, next, period);
	}

	@Test
	void fixedRateWithTimeUnitSubsequentExecution() {
		Instant now = Instant.now();
		long period = 5;
		PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofHours(period));
		trigger.setFixedRate(true);
		Instant next = trigger.nextExecution(context(now, 500, 3000));
		assertApproximateDifference(now, next, (period * 60 * 60 * 1000));
	}

	@Test
	@SuppressWarnings("deprecation")
	void equalsVerification() {
		PeriodicTrigger trigger1 = new PeriodicTrigger(Duration.ofMillis(3000));
		PeriodicTrigger trigger2 = new PeriodicTrigger(Duration.ofMillis(3000));
		assertThat(trigger1.equals(new String("not a trigger"))).isFalse();
		assertThat(trigger1).isNotEqualTo(null);
		assertThat(trigger1).isEqualTo(trigger1);
		assertThat(trigger2).isEqualTo(trigger2);
		assertThat(trigger2).isEqualTo(trigger1);
		trigger2.setInitialDelay(1234);
		assertThat(trigger1).isNotEqualTo(trigger2);
		assertThat(trigger2).isNotEqualTo(trigger1);
		trigger1.setInitialDelay(1234);
		assertThat(trigger2).isEqualTo(trigger1);
		trigger2.setFixedRate(true);
		assertThat(trigger1).isNotEqualTo(trigger2);
		assertThat(trigger2).isNotEqualTo(trigger1);
		trigger1.setFixedRate(true);
		assertThat(trigger2).isEqualTo(trigger1);
		PeriodicTrigger trigger3 = new PeriodicTrigger(Duration.ofSeconds(3));
		trigger3.setInitialDelay(Duration.ofSeconds(7));
		trigger3.setFixedRate(true);
		assertThat(trigger1).isNotEqualTo(trigger3);
		assertThat(trigger3).isNotEqualTo(trigger1);
		trigger1.setInitialDelay(Duration.ofMillis(7000));
		assertThat(trigger3).isEqualTo(trigger1);
	}


	// utility methods

	private static void assertNegligibleDifference(Instant d1, @Nullable Instant d2) {
		assertThat(Duration.between(d1, d2)).isLessThan(Duration.ofMillis(100));
	}

	private static void assertApproximateDifference(Instant lesser, Instant greater, long expected) {
		long diff = greater.toEpochMilli() - lesser.toEpochMilli();
		long variance = Math.abs(expected - diff);
		assertThat(variance).as("expected approximate difference of " + expected +
				", but actual difference was " + diff).isLessThan(100);
	}

	private static TriggerContext context(@Nullable Object scheduled, @Nullable Object actual,
			@Nullable Object completion) {
		return new TestTriggerContext(toInstant(scheduled), toInstant(actual), toInstant(completion));
	}

	@Nullable
	private static Instant toInstant(@Nullable Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Instant) {
			return (Instant) o;
		}
		if (o instanceof Number) {
			return Instant.now()
					.plus(NumberUtils.convertNumberToTargetClass((Number) o, Long.class),
							ChronoUnit.MILLIS);
		}
		throw new IllegalArgumentException(
				"expected Date or Number, but actual type was: " + o.getClass());
	}


	// helper class

	private static class TestTriggerContext implements TriggerContext {

		@Nullable
		private final Instant scheduled;

		@Nullable
		private final Instant actual;

		@Nullable
		private final Instant completion;

		TestTriggerContext(@Nullable Instant scheduled,
				@Nullable Instant actual, @Nullable Instant completion) {

			this.scheduled = scheduled;
			this.actual = actual;
			this.completion = completion;
		}

		@Override
		public Instant lastActualExecution() {
			return this.actual;
		}

		@Override
		public Instant lastCompletion() {
			return this.completion;
		}

		@Override
		public Instant lastScheduledExecution() {
			return this.scheduled;
		}
	}

}
