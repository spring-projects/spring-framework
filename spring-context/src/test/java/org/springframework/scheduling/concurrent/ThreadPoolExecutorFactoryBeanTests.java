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

package org.springframework.scheduling.concurrent;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 */
public class ThreadPoolExecutorFactoryBeanTests {

	@Test
	public void defaultExecutor() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(ExecutorConfig.class);
		ExecutorService executor = context.getBean("executor", ExecutorService.class);

		FutureTask<String> task = new FutureTask<>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "foo";
			}
		});
		executor.execute(task);
		assertEquals("foo", task.get());
	}

	@Test
	public void executorWithPreStartedThreads() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(ExecutorConfigWithPreStartedThreads.class);
		ThreadPoolExecutor executor = context.getBean("childExecutor", ThreadPoolExecutor.class);

		verify(executor).prestartAllCoreThreads();
	}

	@Test
	public void executorWithNoPreStartedThreads() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(ExecutorConfigWithNoPreStartedThreads.class);
		ThreadPoolExecutor executor = context.getBean("childExecutor", ThreadPoolExecutor.class);

		verify(executor, never()).prestartAllCoreThreads();
	}

	@Configuration
	public static class ExecutorConfig {

		@Bean
		public ThreadPoolExecutorFactoryBean executorFactory() {
			return new ThreadPoolExecutorFactoryBean();
		}

		@Bean
		public ExecutorService executor() {
			return executorFactory().getObject();
		}

	}

	@Configuration
	public static class ExecutorConfigWithPreStartedThreads {

		@Bean
		public ThreadPoolExecutorFactoryBean executorChildFactory() {
			ThreadPoolExecutorFactoryBeanMockingChild threadPoolExecutorFactoryBeanMockingChild = new ThreadPoolExecutorFactoryBeanMockingChild();
			threadPoolExecutorFactoryBeanMockingChild.setPrestartAllCoreThreads(true);
			return threadPoolExecutorFactoryBeanMockingChild;
		}

		@Bean
		public ExecutorService childExecutor() {
			return executorChildFactory().getObject();
		}
	}

	@Configuration
	public static class ExecutorConfigWithNoPreStartedThreads {

		@Bean
		public ThreadPoolExecutorFactoryBean executorChildFactory() {
			return new ThreadPoolExecutorFactoryBeanMockingChild();
		}

		@Bean
		public ExecutorService childExecutor() {
			return executorChildFactory().getObject();
		}
	}

	private static class ThreadPoolExecutorFactoryBeanMockingChild extends ThreadPoolExecutorFactoryBean {
		@Override
		protected ThreadPoolExecutor createExecutor(
				int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
				ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

			return mock(ThreadPoolExecutor.class);
		}
	}

}
