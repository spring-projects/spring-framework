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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
class SchedulerBeanDefinitionParserTests {

	private ApplicationContext context;


	@BeforeEach
	void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"schedulerContext.xml", SchedulerBeanDefinitionParserTests.class);
	}

	@Test
	void defaultScheduler() {
		ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) this.context.getBean("defaultScheduler");
		Integer size = (Integer) new DirectFieldAccessor(scheduler).getPropertyValue("poolSize");
		assertThat(size).isEqualTo(1);
	}

	@Test
	void customScheduler() {
		ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) this.context.getBean("customScheduler");
		Integer size = (Integer) new DirectFieldAccessor(scheduler).getPropertyValue("poolSize");
		assertThat(size).isEqualTo(42);
	}

	@Test
	void threadNamePrefix() {
		ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) this.context.getBean("customScheduler");
		assertThat(scheduler.getThreadNamePrefix()).isEqualTo("customScheduler-");
	}

}
