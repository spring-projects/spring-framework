/*
 * Copyright 2002-2012 the original author or authors.
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


package org.springframework.scheduling.concurrent;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Tim Meighen
 * @since 3.2.0
 */
public class ExecutorConfigurationSupportTests {
	@Test
	public void shutdownWaitsForCompletion() throws Exception {
		final ExecutorService executorService = createNiceMock(ExecutorService.class);
		ExecutorConfigurationSupport scheduler = buildExecutorConfigurationSupport(executorService);
		final long expectedMillis = 1500;
		scheduler.setMaxShutdownWaitInMillis(expectedMillis);
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.initialize();
		executorService.shutdown();
		expect(executorService.awaitTermination(expectedMillis, TimeUnit.MILLISECONDS)).andReturn(true);
		replay(executorService);
		scheduler.shutdown();
		verify(executorService);
	}

	@Test
	public void shutdownWaitsForCompletionButFails() throws Exception {
		final ExecutorService executorService = createNiceMock(ExecutorService.class);
		ExecutorConfigurationSupport scheduler = buildExecutorConfigurationSupport(executorService);
		final long expectedMillis = 1500;
		scheduler.setMaxShutdownWaitInMillis(expectedMillis);
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setBeanName("TEST SCHEDULER");
		scheduler.initialize();
		executorService.shutdown();
		expect(executorService.awaitTermination(expectedMillis, TimeUnit.MILLISECONDS)).andReturn(false);
		final List<Runnable> unterminatedTasks = Collections.singletonList(createNiceMock(Runnable.class));
		expect(executorService.shutdownNow()).andReturn(unterminatedTasks);
		replay(executorService);
		scheduler.shutdown();
		verify(executorService);
	}


	// utility methods

	private static ExecutorConfigurationSupport buildExecutorConfigurationSupport(final ExecutorService executorService) {
		return new ExecutorConfigurationSupport() {
				@Override
				protected ExecutorService initializeExecutor(final ThreadFactory threadFactory, final RejectedExecutionHandler rejectedExecutionHandler) {
					return executorService;
				}
			};
	}
}
