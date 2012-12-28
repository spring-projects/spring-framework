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

package org.springframework.scheduling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.junit.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.startsWith;

import static org.junit.Assert.*;

/**
 * Tests use of @EnableAsync on @Configuration classes.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class EnableAsyncTests {

	@Test
	public void proxyingOccurs() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfig.class);
		ctx.refresh();

		AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
		assertThat(AopUtils.isAopProxy(asyncBean), is(true));
		asyncBean.work();
	}


	@Configuration
	@EnableAsync
	static class AsyncConfig {
		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}
	}


	@SuppressWarnings("unchecked")
	@Test
	public void withAsyncBeanWithExecutorQualifiedByName() throws ExecutionException, InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncWithExecutorQualifiedByNameConfig.class);
		ctx.refresh();

		AsyncBeanWithExecutorQualifiedByName asyncBean = ctx.getBean(AsyncBeanWithExecutorQualifiedByName.class);
		Future<Thread> workerThread0 = asyncBean.work0();
		assertThat(workerThread0.get().getName(), not(anyOf(startsWith("e1-"), startsWith("otherExecutor-"))));
		Future<Thread> workerThread = asyncBean.work();
		assertThat(workerThread.get().getName(), startsWith("e1-"));
		Future<Thread> workerThread2 = asyncBean.work2();
		assertThat(workerThread2.get().getName(), startsWith("otherExecutor-"));
		Future<Thread> workerThread3 = asyncBean.work3();
		assertThat(workerThread3.get().getName(), startsWith("otherExecutor-"));
	}


	static class AsyncBeanWithExecutorQualifiedByName {
		@Async
		public Future<Thread> work0() {
			return new AsyncResult<Thread>(Thread.currentThread());
		}

		@Async("e1")
		public Future<Thread> work() {
			return new AsyncResult<Thread>(Thread.currentThread());
		}

		@Async("otherExecutor")
		public Future<Thread> work2() {
			return new AsyncResult<Thread>(Thread.currentThread());
		}

		@Async("e2")
		public Future<Thread> work3() {
			return new AsyncResult<Thread>(Thread.currentThread());
		}
	}


	static class AsyncBean {
		private Thread threadOfExecution;

		@Async
		public void work() {
			this.threadOfExecution = Thread.currentThread();
		}

		public Thread getThreadOfExecution() {
			return threadOfExecution;
		}
	}


	@Test
	public void asyncProcessorIsOrderedLowestPrecedenceByDefault() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfig.class);
		ctx.refresh();

		AsyncAnnotationBeanPostProcessor bpp = ctx.getBean(AsyncAnnotationBeanPostProcessor.class);
		assertThat(bpp.getOrder(), is(Ordered.LOWEST_PRECEDENCE));
	}


	@Test
	public void orderAttributeIsPropagated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(OrderedAsyncConfig.class);
		ctx.refresh();

		AsyncAnnotationBeanPostProcessor bpp = ctx.getBean(AsyncAnnotationBeanPostProcessor.class);
		assertThat(bpp.getOrder(), is(Ordered.HIGHEST_PRECEDENCE));
	}


	@Configuration
	@EnableAsync(order=Ordered.HIGHEST_PRECEDENCE)
	static class OrderedAsyncConfig {
		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}
	}


	@Test
	public void customAsyncAnnotationIsPropagated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CustomAsyncAnnotationConfig.class);
		ctx.refresh();

		Object bean = ctx.getBean(CustomAsyncBean.class);
		assertTrue(AopUtils.isAopProxy(bean));
		boolean isAsyncAdvised = false;
		for (Advisor advisor : ((Advised)bean).getAdvisors()) {
			if (advisor instanceof AsyncAnnotationAdvisor) {
				isAsyncAdvised = true;
				break;
			}
		}
		assertTrue("bean was not async advised as expected", isAsyncAdvised);
	}


	@Configuration
	@EnableAsync(annotation=CustomAsync.class)
	static class CustomAsyncAnnotationConfig {
		@Bean
		public CustomAsyncBean asyncBean() {
			return new CustomAsyncBean();
		}
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface CustomAsync {
	}


	static class CustomAsyncBean {
		@CustomAsync
		public void work() {
		}
	}


	/**
	 * Fails with classpath errors on trying to classload AnnotationAsyncExecutionAspect
	 */
	@Test(expected=BeanDefinitionStoreException.class)
	public void aspectModeAspectJAttemptsToRegisterAsyncAspect() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AspectJAsyncAnnotationConfig.class);
		ctx.refresh();
	}


	@Configuration
	@EnableAsync(mode=AdviceMode.ASPECTJ)
	static class AspectJAsyncAnnotationConfig {
		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}
	}


	@Test
	public void customExecutorIsPropagated() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CustomExecutorAsyncConfig.class);
		ctx.refresh();

		AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
		asyncBean.work();
		Thread.sleep(500);
		ctx.close();
		assertThat(asyncBean.getThreadOfExecution().getName(), startsWith("Custom-"));
	}


	@Configuration
	@EnableAsync
	static class CustomExecutorAsyncConfig implements AsyncConfigurer {
		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}

		@Override
		public Executor getAsyncExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix("Custom-");
			executor.initialize();
			return executor;
		}
	}


	@Configuration
	@EnableAsync
	static class AsyncWithExecutorQualifiedByNameConfig {
		@Bean
		public AsyncBeanWithExecutorQualifiedByName asyncBean() {
			return new AsyncBeanWithExecutorQualifiedByName();
		}

		@Bean
		public Executor e1() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			return executor;
		}

		@Bean
		@Qualifier("e2")
		public Executor otherExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			return executor;
		}
	}
}
