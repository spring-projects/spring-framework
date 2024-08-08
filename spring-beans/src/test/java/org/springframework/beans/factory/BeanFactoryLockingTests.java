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

package org.springframework.beans.factory;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 6.2
 */
class BeanFactoryLockingTests {

	@Test
	void fallbackForThreadDuringInitialization() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("bean1",
				new RootBeanDefinition(ThreadDuringInitialization.class));
		beanFactory.registerBeanDefinition("bean2",
				new RootBeanDefinition(TestBean.class, () -> new TestBean("tb")));
		beanFactory.getBean(ThreadDuringInitialization.class);
	}


	static class ThreadDuringInitialization implements BeanFactoryAware, InitializingBean {

		private BeanFactory beanFactory;

		private volatile boolean initialized;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			Thread thread = new Thread(() -> {
				// Fail for circular reference from other thread
				assertThatExceptionOfType(BeanCurrentlyInCreationException.class).isThrownBy(() ->
						beanFactory.getBean(ThreadDuringInitialization.class));
				// Leniently create unrelated other bean outside of singleton lock
				assertThat(beanFactory.getBean(TestBean.class).getName()).isEqualTo("tb");
				// Creation attempt in other thread was successful
				initialized = true;
			});
			thread.start();
			thread.join();
			if (!initialized) {
				throw new IllegalStateException("Thread not executed");
			}
		}
	}

}
