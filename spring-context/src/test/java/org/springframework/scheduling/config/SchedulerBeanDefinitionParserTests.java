/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
