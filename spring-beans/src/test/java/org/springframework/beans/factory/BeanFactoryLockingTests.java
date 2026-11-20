/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

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
		beanFactory.preInstantiateSingletons();
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
