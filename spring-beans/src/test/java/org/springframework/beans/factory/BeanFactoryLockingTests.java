/*
 * Copyright 2002-present the original author or authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
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

	@Test  // gh-37382 follow-up: no map lock held while matching bean types
	void noDeadlockBetweenTypeLookupAndSingletonCreation() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		CountDownLatch holderInCreation = new CountDownLatch(1);
		CountDownLatch lookupInProgress = new CountDownLatch(1);

		// Lazy singleton whose creation (under the singleton lock) performs a type lookup
		RootBeanDefinition holder = new RootBeanDefinition(Object.class, () -> {
			holderInCreation.countDown();
			await(lookupInProgress);
			beanFactory.getBeanNamesForType(TestBean.class);
			return new Object();
		});
		holder.setLazyInit(true);
		beanFactory.registerBeanDefinition("holder", holder);
		// FactoryBean whose object type can only be determined through instantiation
		beanFactory.registerBeanDefinition("factoryBean", new RootBeanDefinition(UntypedFactoryBean.class));
		beanFactory.addBeanPostProcessor(new SmartInstantiationAwareBeanPostProcessor() {
			@Override
			public @Nullable Class<?> predictBeanType(Class<?> beanClass, String beanName) {
				if (beanName.equals("factoryBean")) {
					lookupInProgress.countDown();
				}
				return null;
			}
		});
		beanFactory.freezeConfiguration();

		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread creator = new Thread(() -> beanFactory.getBean("holder"));
		Thread lookup = new Thread(() -> {
			await(holderInCreation);
			beanFactory.getBeanNamesForType(TestBean.class);
		});
		for (Thread thread : new Thread[] {creator, lookup}) {
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler((t, ex) -> failure.set(ex));
			thread.start();
		}
		creator.join(TimeUnit.SECONDS.toMillis(10));
		lookup.join(TimeUnit.SECONDS.toMillis(10));

		assertThat(creator.isAlive()).as("creator thread deadlocked").isFalse();
		assertThat(lookup.isAlive()).as("lookup thread deadlocked").isFalse();
		assertThat(failure.get()).isNull();
		assertThat(beanFactory.getBeanNamesForType(TestBean.class)).containsExactly("factoryBean");
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Latch timed out");
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
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


	@SuppressWarnings("rawtypes")
	static class UntypedFactoryBean implements FactoryBean {

		@Override
		public Object getObject() {
			return new TestBean();
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}
	}

}
