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

package org.springframework.scheduling.config;

import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CustomizableThreadCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
class ExecutorBeanDefinitionParserTests {

	private ApplicationContext context;


	@BeforeEach
	void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"executorContext.xml", ExecutorBeanDefinitionParserTests.class);
	}


	@Test
	void defaultExecutor() throws Exception {
		ThreadPoolTaskExecutor executor = this.context.getBean("default", ThreadPoolTaskExecutor.class);
		assertThat(getCorePoolSize(executor)).isEqualTo(1);
		assertThat(getMaxPoolSize(executor)).isEqualTo(Integer.MAX_VALUE);
		assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
		assertThat(getKeepAliveSeconds(executor)).isEqualTo(60);
		assertThat(getAllowCoreThreadTimeOut(executor)).isFalse();

		FutureTask<String> task = new FutureTask<>(() -> "foo");
		executor.execute(task);
		assertThat(task.get()).isEqualTo("foo");
	}

	@Test
	void singleSize() {
		Object executor = this.context.getBean("singleSize");
		assertThat(getCorePoolSize(executor)).isEqualTo(42);
		assertThat(getMaxPoolSize(executor)).isEqualTo(42);
	}

	@Test
	void invalidPoolSize() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				this.context.getBean("invalidPoolSize"));
	}

	@Test
	void rangeWithBoundedQueue() {
		Object executor = this.context.getBean("rangeWithBoundedQueue");
		assertThat(getCorePoolSize(executor)).isEqualTo(7);
		assertThat(getMaxPoolSize(executor)).isEqualTo(42);
		assertThat(getQueueCapacity(executor)).isEqualTo(11);
	}

	@Test
	void rangeWithUnboundedQueue() {
		Object executor = this.context.getBean("rangeWithUnboundedQueue");
		assertThat(getCorePoolSize(executor)).isEqualTo(9);
		assertThat(getMaxPoolSize(executor)).isEqualTo(9);
		assertThat(getKeepAliveSeconds(executor)).isEqualTo(37);
		assertThat(getAllowCoreThreadTimeOut(executor)).isTrue();
		assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void propertyPlaceholderWithSingleSize() {
		Object executor = this.context.getBean("propertyPlaceholderWithSingleSize");
		assertThat(getCorePoolSize(executor)).isEqualTo(123);
		assertThat(getMaxPoolSize(executor)).isEqualTo(123);
		assertThat(getKeepAliveSeconds(executor)).isEqualTo(60);
		assertThat(getAllowCoreThreadTimeOut(executor)).isFalse();
		assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void propertyPlaceholderWithRange() {
		Object executor = this.context.getBean("propertyPlaceholderWithRange");
		assertThat(getCorePoolSize(executor)).isEqualTo(5);
		assertThat(getMaxPoolSize(executor)).isEqualTo(25);
		assertThat(getAllowCoreThreadTimeOut(executor)).isFalse();
		assertThat(getQueueCapacity(executor)).isEqualTo(10);
	}

	@Test
	void propertyPlaceholderWithRangeAndCoreThreadTimeout() {
		Object executor = this.context.getBean("propertyPlaceholderWithRangeAndCoreThreadTimeout");
		assertThat(getCorePoolSize(executor)).isEqualTo(99);
		assertThat(getMaxPoolSize(executor)).isEqualTo(99);
		assertThat(getAllowCoreThreadTimeOut(executor)).isTrue();
	}

	@Test
	void propertyPlaceholderWithInvalidPoolSize() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				this.context.getBean("propertyPlaceholderWithInvalidPoolSize"));
	}

	@Test
	void threadNamePrefix() {
		CustomizableThreadCreator executor = this.context.getBean("default", CustomizableThreadCreator.class);
		assertThat(executor.getThreadNamePrefix()).isEqualTo("default-");
	}

	@Test
	void typeCheck() {
		assertThat(this.context.isTypeMatch("default", Executor.class)).isTrue();
		assertThat(this.context.isTypeMatch("default", TaskExecutor.class)).isTrue();
		assertThat(this.context.isTypeMatch("default", ThreadPoolTaskExecutor.class)).isTrue();
	}


	private int getCorePoolSize(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("corePoolSize");
	}

	private int getMaxPoolSize(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("maxPoolSize");
	}

	private int getQueueCapacity(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("queueCapacity");
	}

	private int getKeepAliveSeconds(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("keepAliveSeconds");
	}

	private boolean getAllowCoreThreadTimeOut(Object executor) {
		return (Boolean) new DirectFieldAccessor(executor).getPropertyValue("allowCoreThreadTimeOut");
	}

}
