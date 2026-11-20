/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ThreadPoolExecutorFactoryBean}.
 *
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
	void executorWithDefaultSettingsDoesNotPrestartAllCoreThreads() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("taskExecutor", ThreadPoolExecutorFactoryBean.class, TestThreadPoolExecutorFactoryBean::new);
		context.refresh();
		ThreadPoolExecutor threadPoolExecutor = context.getBean(ThreadPoolExecutor.class);
		verify(threadPoolExecutor, never()).prestartAllCoreThreads();
		context.close();
	}

	@Test
	void executorWithPrestartAllCoreThreads() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("taskExecutor", ThreadPoolExecutorFactoryBean.class, () -> {
			TestThreadPoolExecutorFactoryBean factoryBean = new TestThreadPoolExecutorFactoryBean();
			factoryBean.setPrestartAllCoreThreads(true);
			return factoryBean;
		});
		context.refresh();
		ThreadPoolExecutor threadPoolExecutor = context.getBean(ThreadPoolExecutor.class);
		verify(threadPoolExecutor).prestartAllCoreThreads();
		context.close();
	}


	@Configuration
	static class ExecutorConfig {

		@Bean
		ThreadPoolExecutorFactoryBean executor() {
			return new ThreadPoolExecutorFactoryBean();
		}
	}


	@SuppressWarnings("serial")
	private static class TestThreadPoolExecutorFactoryBean extends ThreadPoolExecutorFactoryBean {

		@Override
		protected ThreadPoolExecutor createExecutor(
				int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
				ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

			return mock();
		}
	}

}
