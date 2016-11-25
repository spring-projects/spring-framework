/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 */
class ThreadPoolExecutorFactoryBeanTests {

	@Test
	void defaultExecutor() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExecutorConfig.class);
		ExecutorService executor = context.getBean(ExecutorService.class);

		FutureTask<String> task = new FutureTask<>(() -> "foo");
		executor.execute(task);
		assertThat(task.get()).isEqualTo("foo");
		context.close();
	}

	@Test
	public void executorWithPreStartedThreads() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExecutorConfigWithPreStartedThreads.class);
		ThreadPoolExecutor executor = context.getBean("childExecutor", ThreadPoolExecutor.class);

		verify(executor).prestartAllCoreThreads();
	}

	@Test
	public void executorWithNoPreStartedThreads() throws Exception {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExecutorConfigWithNoPreStartedThreads.class);
		ThreadPoolExecutor executor = context.getBean("childExecutor", ThreadPoolExecutor.class);

		verify(executor, never()).prestartAllCoreThreads();
	}

	@Configuration
	static class ExecutorConfig {

		@Bean
		ThreadPoolExecutorFactoryBean executor() {
			return new ThreadPoolExecutorFactoryBean();
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
