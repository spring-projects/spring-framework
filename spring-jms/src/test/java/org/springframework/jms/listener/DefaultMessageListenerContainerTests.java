/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms.listener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Stephane Nicoll
 */
public class DefaultMessageListenerContainerTests {

	@Test
	public void applyBackOff() {
		BackOff backOff = mock(BackOff.class);
		BackOffExecution execution = mock(BackOffExecution.class);
		given(execution.nextBackOff()).willReturn(BackOffExecution.STOP);
		given(backOff.start()).willReturn(execution);

		DefaultMessageListenerContainer container = createContainer(createFailingContainerFactory());
		container.setBackOff(backOff);
		container.start();
		assertEquals(true, container.isRunning());

		container.refreshConnectionUntilSuccessful();

		assertEquals(false, container.isRunning());
		verify(backOff).start();
		verify(execution).nextBackOff();
	}

	@Test
	public void applyBackOffRetry() {
		BackOff backOff = mock(BackOff.class);
		BackOffExecution execution = mock(BackOffExecution.class);
		given(execution.nextBackOff()).willReturn(50L, BackOffExecution.STOP);
		given(backOff.start()).willReturn(execution);

		DefaultMessageListenerContainer container = createContainer(createFailingContainerFactory());
		container.setBackOff(backOff);
		container.start();
		container.refreshConnectionUntilSuccessful();

		assertEquals(false, container.isRunning());
		verify(backOff).start();
		verify(execution, times(2)).nextBackOff();
	}

	@Test
	public void recoverResetBackOff() {
		BackOff backOff = mock(BackOff.class);
		BackOffExecution execution = mock(BackOffExecution.class);
		given(execution.nextBackOff()).willReturn(50L, 50L, 50L); // 3 attempts max
		given(backOff.start()).willReturn(execution);

		DefaultMessageListenerContainer container = createContainer(createRecoverableContainerFactory(1));
		container.setBackOff(backOff);
		container.start();
		container.refreshConnectionUntilSuccessful();

		assertEquals(true, container.isRunning());
		verify(backOff).start();
		verify(execution, times(1)).nextBackOff(); // only on attempt as the second one lead to a recovery
	}

	@Test
	public void runnableIsInvokedEvenIfContainerIsNotRunning() throws InterruptedException {
		DefaultMessageListenerContainer container = createRunningContainer();
		container.stop();

		// container is stopped but should nevertheless invoke the runnable argument
		TestRunnable runnable2 = new TestRunnable();
		container.stop(runnable2);
		runnable2.waitForCompletion();
	}


	private DefaultMessageListenerContainer createRunningContainer() {
		DefaultMessageListenerContainer container = createContainer(createSuccessfulConnectionFactory());
		container.afterPropertiesSet();
		container.start();
		return container;
	}

	private DefaultMessageListenerContainer createContainer(ConnectionFactory connectionFactory) {
		Destination destination = new Destination() {};

		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_NONE);
		container.setDestination(destination);
		return container;
	}

	private ConnectionFactory createFailingContainerFactory() {
		try {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			given(connectionFactory.createConnection()).will(new Answer<Object>() {
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					throw new JMSException("Test exception");
				}
			});
			return connectionFactory;
		}
		catch (JMSException ex) {
			throw new IllegalStateException(ex);  // never happen
		}
	}

	private ConnectionFactory createRecoverableContainerFactory(final int failingAttempts) {
		try {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			given(connectionFactory.createConnection()).will(new Answer<Object>() {
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

	private ConnectionFactory createSuccessfulConnectionFactory() {
		try {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			given(connectionFactory.createConnection()).willReturn(mock(Connection.class));
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

		public void waitForCompletion() throws InterruptedException {
			this.countDownLatch.await(2, TimeUnit.SECONDS);
			assertEquals("callback was not invoked", 0, this.countDownLatch.getCount());
		}
	}

}
