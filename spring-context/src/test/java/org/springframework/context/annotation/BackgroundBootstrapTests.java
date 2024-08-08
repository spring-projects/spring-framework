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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.springframework.context.annotation.Bean.Bootstrap.BACKGROUND;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Juergen Hoeller
 * @since 6.2
 */
class BackgroundBootstrapTests {

	@Test
	@Timeout(5)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithCustomExecutor() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CustomExecutorBeanConfig.class);
		ctx.getBean("testBean1", TestBean.class);
		ctx.getBean("testBean2", TestBean.class);
		ctx.getBean("testBean3", TestBean.class);
		ctx.close();
	}


	@Configuration
	static class CustomExecutorBeanConfig {

		@Bean
		public ThreadPoolTaskExecutor bootstrapExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix("Custom-");
			executor.setCorePoolSize(2);
			executor.initialize();
			return executor;
		}

		@Bean(bootstrap = BACKGROUND) @DependsOn("testBean3")
		public TestBean testBean1(TestBean testBean3) throws InterruptedException{
			Thread.sleep(3000);
			return new TestBean();
		}

		@Bean(bootstrap = BACKGROUND) @Lazy
		public TestBean testBean2() throws InterruptedException {
			Thread.sleep(3000);
			return new TestBean();
		}

		@Bean @Lazy
		public TestBean testBean3() {
			return new TestBean();
		}

		@Bean
		public String dependent(@Lazy TestBean testBean1, @Lazy TestBean testBean2, @Lazy TestBean testBean3) {
			return "";
		}
	}

}
