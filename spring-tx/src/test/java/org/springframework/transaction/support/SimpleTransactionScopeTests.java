/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.support;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
public class SimpleTransactionScopeTests {

	@Test
	@SuppressWarnings("resource")
	public void getFromScope() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerScope("tx", new SimpleTransactionScope());

		GenericBeanDefinition bd1 = new GenericBeanDefinition();
		bd1.setBeanClass(TestBean.class);
		bd1.setScope("tx");
		bd1.setPrimary(true);
		context.registerBeanDefinition("txScopedObject1", bd1);

		GenericBeanDefinition bd2 = new GenericBeanDefinition();
		bd2.setBeanClass(DerivedTestBean.class);
		bd2.setScope("tx");
		context.registerBeanDefinition("txScopedObject2", bd2);

		context.refresh();

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				context.getBean(TestBean.class))
			.withCauseInstanceOf(IllegalStateException.class);

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				context.getBean(DerivedTestBean.class))
			.withCauseInstanceOf(IllegalStateException.class);

		TestBean bean1 = null;
		DerivedTestBean bean2 = null;
		DerivedTestBean bean2a = null;
		DerivedTestBean bean2b = null;

		TransactionSynchronizationManager.initSynchronization();
		try {
			bean1 = context.getBean(TestBean.class);
			assertThat(context.getBean(TestBean.class)).isSameAs(bean1);

			bean2 = context.getBean(DerivedTestBean.class);
			assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2);
			context.getBeanFactory().destroyScopedBean("txScopedObject2");
			assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
			assertThat(bean2.wasDestroyed()).isTrue();

			bean2a = context.getBean(DerivedTestBean.class);
			assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2a);
			assertThat(bean2a).isNotSameAs(bean2);
			context.getBeanFactory().getRegisteredScope("tx").remove("txScopedObject2");
			assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
			assertThat(bean2a.wasDestroyed()).isFalse();

			bean2b = context.getBean(DerivedTestBean.class);
			assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2b);
			assertThat(bean2b).isNotSameAs(bean2);
			assertThat(bean2b).isNotSameAs(bean2a);
		}
		finally {
			TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			TransactionSynchronizationManager.clearSynchronization();
		}

		assertThat(bean2a.wasDestroyed()).isFalse();
		assertThat(bean2b.wasDestroyed()).isTrue();
		assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				context.getBean(TestBean.class))
			.withCauseInstanceOf(IllegalStateException.class);

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				context.getBean(DerivedTestBean.class))
			.withCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	public void getWithTransactionManager() throws Exception {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.getBeanFactory().registerScope("tx", new SimpleTransactionScope());

			GenericBeanDefinition bd1 = new GenericBeanDefinition();
			bd1.setBeanClass(TestBean.class);
			bd1.setScope("tx");
			bd1.setPrimary(true);
			context.registerBeanDefinition("txScopedObject1", bd1);

			GenericBeanDefinition bd2 = new GenericBeanDefinition();
			bd2.setBeanClass(DerivedTestBean.class);
			bd2.setScope("tx");
			context.registerBeanDefinition("txScopedObject2", bd2);

			context.refresh();

			CallCountingTransactionManager tm = new CallCountingTransactionManager();
			TransactionTemplate tt = new TransactionTemplate(tm);
			Set<DerivedTestBean> finallyDestroy = new HashSet<>();

			tt.execute(status -> {
				TestBean bean1 = context.getBean(TestBean.class);
				assertThat(context.getBean(TestBean.class)).isSameAs(bean1);

				DerivedTestBean bean2 = context.getBean(DerivedTestBean.class);
				assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2);
				context.getBeanFactory().destroyScopedBean("txScopedObject2");
				assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
				assertThat(bean2.wasDestroyed()).isTrue();

				DerivedTestBean bean2a = context.getBean(DerivedTestBean.class);
				assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2a);
				assertThat(bean2a).isNotSameAs(bean2);
				context.getBeanFactory().getRegisteredScope("tx").remove("txScopedObject2");
				assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
				assertThat(bean2a.wasDestroyed()).isFalse();

				DerivedTestBean bean2b = context.getBean(DerivedTestBean.class);
				finallyDestroy.add(bean2b);
				assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2b);
				assertThat(bean2b).isNotSameAs(bean2);
				assertThat(bean2b).isNotSameAs(bean2a);

				Set<DerivedTestBean> immediatelyDestroy = new HashSet<>();
				TransactionTemplate tt2 = new TransactionTemplate(tm);
				tt2.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
				tt2.execute(status2 -> {
					DerivedTestBean bean2c = context.getBean(DerivedTestBean.class);
					immediatelyDestroy.add(bean2c);
					assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2c);
					assertThat(bean2c).isNotSameAs(bean2);
					assertThat(bean2c).isNotSameAs(bean2a);
					assertThat(bean2c).isNotSameAs(bean2b);
					return null;
				});
				assertThat(immediatelyDestroy.iterator().next().wasDestroyed()).isTrue();
				assertThat(bean2b.wasDestroyed()).isFalse();

				return null;
			});

			assertThat(finallyDestroy.iterator().next().wasDestroyed()).isTrue();
		}
	}

}
