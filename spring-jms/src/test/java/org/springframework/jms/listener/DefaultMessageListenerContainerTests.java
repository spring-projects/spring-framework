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

package org.springframework.jms.listener;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultMessageListenerContainer}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class DefaultMessageListenerContainerTests {

	@Test
	void applyBackOff() {
		BackOff backOff = mock();
		BackOffExecution execution = mock();
		given(execution.nextBackOff()).willReturn(BackOffExecution.STOP);
		given(backOff.start()).willReturn(execution);

		DefaultMessageListenerContainer container = createContainer(createFailingContainerFactory());
		container.setBackOff(backOff);
		container.start();
		assertThat(container.isRunning()).isTrue();

		container.refreshConnectionUntilSuccessful();

		assertThat(container.isRunning()).isFalse();
		verify(backOff).start();
		verify(execution).nextBackOff();

		container.destroy();
	}

	@Test
	void applyBackOffRetry() {
		BackOff backOff = mock();
		BackOffExecution execution = mock();
		given(execution.nextBackOff()).willReturn(50L, BackOffExecution.STOP);
		given(backOff.start()).willReturn(execution);

		DefaultMessageListenerContainer container = createContainer(createFailingContainerFactory());
		container.setBackOff(backOff);
		container.start();
		container.refreshConnectionUntilSuccessful();

		assertThat(container.isRunning()).isFalse();
		verify(backOff).start();
		verify(execution, times(2)).nextBackOff();

		container.destroy();
	}

	@Test
	void recoverResetBackOff() {
		BackOff backOff = mock();
		BackOffExecution execution = mock();
		given(execution.nextBackOff()).willReturn(50L, 50L, 50L);  // 3 attempts max
		given(backOff.start()).willReturn(execution);

		DefaultMessageListenerContainer container = createContainer(createRecoverableContainerFactory(1));
		container.setBackOff(backOff);
		container.start();
		container.refreshConnectionUntilSuccessful();

		assertThat(container.isRunning()).isTrue();
		verify(backOff).start();
		verify(execution, times(1)).nextBackOff();  // only on attempt as the second one lead to a recovery

		container.destroy();
	}

	@Test
	void stopAndRestart() {
		DefaultMessageListenerContainer container = createRunningContainer();
		container.stop();

		container.start();
		container.destroy();
	}

	@Test
	void stopWithCallbackAndRestart() throws InterruptedException {
		DefaultMessageListenerContainer container = createRunningContainer();

		TestRunnable stopCallback = new TestRunnable();
		container.stop(stopCallback);
		stopCallback.waitForCompletion();

		container.start();
		container.destroy();
	}

	@Test
	void stopCallbackIsInvokedEvenIfContainerIsNotRunning() throws InterruptedException {
		DefaultMessageListenerContainer container = createRunningContainer();
		container.stop();

		// container is stopped but should nevertheless invoke the runnable argument
		TestRunnable stopCallback = new TestRunnable();
		container.stop(stopCallback);
		stopCallback.waitForCompletion();

		container.destroy();
	}

	@Test
	void setCacheLevelNameToUnsupportedValues() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		assertThatIllegalArgumentException().isThrownBy(() -> container.setCacheLevelName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> container.setCacheLevelName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> container.setCacheLevelName("bogus"));
	}

	/**
	 * This test effectively verifies that the internal 'constants' map is properly
	 * configured for all cache constants defined in {@link DefaultMessageListenerContainer}.
	 */
	@Test
	void setCacheLevelNameToAllSupportedValues() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		Set<Integer> uniqueValues = new HashSet<>();
		streamCacheConstants()
				.forEach(name -> {
					container.setCacheLevelName(name);
					int cacheLevel = container.getCacheLevel();
					assertThat(cacheLevel).isBetween(0, 4);
					uniqueValues.add(cacheLevel);
				});
		assertThat(uniqueValues).hasSize(5);
	}

	@Test
	void setCacheLevel() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();

		assertThatIllegalArgumentException().isThrownBy(() -> container.setCacheLevel(999));

		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_NONE);
		assertThat(container.getCacheLevel()).isEqualTo(DefaultMessageListenerContainer.CACHE_NONE);

		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONNECTION);
		assertThat(container.getCacheLevel()).isEqualTo(DefaultMessageListenerContainer.CACHE_CONNECTION);

		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_SESSION);
		assertThat(container.getCacheLevel()).isEqualTo(DefaultMessageListenerContainer.CACHE_SESSION);

		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
		assertThat(container.getCacheLevel()).isEqualTo(DefaultMessageListenerContainer.CACHE_CONSUMER);

		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_AUTO);
		assertThat(container.getCacheLevel()).isEqualTo(DefaultMessageListenerContainer.CACHE_AUTO);
	}


	private static Stream<String> streamCacheConstants() {
		return Arrays.stream(DefaultMessageListenerContainer.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.filter(field -> field.getName().startsWith("CACHE_"))
				.map(Field::getName);
	}

	private static DefaultMessageListenerContainer createRunningContainer() {
		DefaultMessageListenerContainer container = createContainer(createSuccessfulConnectionFactory());
		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONNECTION);
		container.setBackOff(new FixedBackOff(100, 1));
		container.afterPropertiesSet();
		container.start();
		return container;
	}

	private static ConnectionFactory createSuccessfulConnectionFactory() {
		try {
			ConnectionFactory connectionFactory = mock();
			given(connectionFactory.createConnection()).willReturn(mock());
			return connectionFactory;
		}
		catch (JMSException ex) {
			throw new IllegalStateException(ex);  // never happen
		}
	}

	private static DefaultMessageListenerContainer createContainer(ConnectionFactory connectionFactory) {
		Destination destination = new Destination() {};

		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_NONE);
		container.setDestination(destination);
		return container;
	}

	private static ConnectionFactory createFailingContainerFactory() {
		try {
			ConnectionFactory connectionFactory = mock();
			given(connectionFactory.createConnection()).will(invocation -> {
				throw new JMSException("Test exception");
			});
			return connectionFactory;
		}
		catch (JMSException ex) {
			throw new IllegalStateException(ex);  // never happen
		}
	}

	private static ConnectionFactory createRecoverableContainerFactory(final int failingAttempts) {
		try {
			ConnectionFactory connectionFactory = mock();
			given(connectionFactory.createConnection()).will(new Answer<>() {
				int currentAttempts = 0;
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					currentAttempts++;
					if (currentAttempts <= failingAttempts) {
						throw new JMSException("Test exception (attempt " + currentAttempts + ")");
					}
					else {
						return mock(Connection.class);
					}
				}
			});
			return connectionFactory;
		}
		catch (JMSException ex) {
			throw new IllegalStateException(ex);  // never happen
		}
	}


	private static class TestRunnable implements Runnable {

		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		@Override
		public void run() {
			this.countDownLatch.countDown();
		}

		void waitForCompletion() throws InterruptedException {
			this.countDownLatch.await(1, TimeUnit.SECONDS);
			assertThat(this.countDownLatch.getCount()).as("callback was not invoked").isEqualTo(0);
		}
	}

}
