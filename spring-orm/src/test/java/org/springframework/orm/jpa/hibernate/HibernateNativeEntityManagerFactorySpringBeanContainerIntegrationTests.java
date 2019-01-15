/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.orm.jpa.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.hibernate.beans.*;

import static org.junit.Assert.*;

/**
 * Hibernate-specific SpringBeanContainer integration tests.
 *
 * @author Yoann Rodiere
 */
public class HibernateNativeEntityManagerFactorySpringBeanContainerIntegrationTests
		extends AbstractEntityManagerFactoryIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager-native.xml",
				"/org/springframework/orm/jpa/memdb.xml", "/org/springframework/orm/jpa/inject.xml",
				"/org/springframework/orm/jpa/hibernate/inject-hibernate-spring-bean-container-tests.xml"};
	}

	private ManagedBeanRegistry getManagedBeanRegistry() {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		ServiceRegistry serviceRegistry = sessionFactory.getSessionFactoryOptions().getServiceRegistry();
		return serviceRegistry.requireService( ManagedBeanRegistry.class );
	}

	private BeanContainer getBeanContainer() {
		return getManagedBeanRegistry().getBeanContainer();
	}


	@Test
	public void testCanRetrieveBeanByTypeWithJpaCompliantOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertNotNull(beanContainer);

		ContainedBean<SinglePrototypeInSpringContextTestBean> bean = beanContainer.getBean(
				SinglePrototypeInSpringContextTestBean.class,
				JpaLifecycleOptions.INSTANCE,
				IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertNotNull(bean);
		SinglePrototypeInSpringContextTestBean instance = bean.getBeanInstance();
		assertNotNull(instance);
		assertSame(applicationContext, instance.getApplicationContext());
	}

	@Test
	public void testCanRetrieveBeanByNameWithJpaCompliantOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertNotNull(beanContainer);

		ContainedBean<MultiplePrototypesInSpringContextTestBean> bean = beanContainer.getBean(
				"multiple-1", MultiplePrototypesInSpringContextTestBean.class,
				JpaLifecycleOptions.INSTANCE,
				IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertNotNull(bean);
		MultiplePrototypesInSpringContextTestBean instance = bean.getBeanInstance();
		assertNotNull(instance);
		assertEquals("multiple-1", instance.getName());
		assertSame(applicationContext, instance.getApplicationContext());
	}

	@Test
	public void testCanRetrieveBeanByTypeWithNativeOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertNotNull(beanContainer);

		ContainedBean<SinglePrototypeInSpringContextTestBean> bean = beanContainer.getBean(
				SinglePrototypeInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE,
				IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertNotNull(bean);
		SinglePrototypeInSpringContextTestBean instance = bean.getBeanInstance();
		assertNotNull(instance);
		assertEquals("single", instance.getName());
		assertSame(applicationContext, instance.getApplicationContext());

		ContainedBean<SinglePrototypeInSpringContextTestBean> bean2 = beanContainer.getBean(
				SinglePrototypeInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE,
				IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertNotNull(bean2);
		SinglePrototypeInSpringContextTestBean instance2 = bean2.getBeanInstance();
		assertNotNull(instance2);
		// Due to the lifecycle options, and because the bean has the "prototype" scope, we should not return the same instance
		assertNotSame(instance, instance2);
	}

	@Test
	public void testCanRetrieveBeanByNameWithNativeOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertNotNull(beanContainer);

		ContainedBean<MultiplePrototypesInSpringContextTestBean> bean = beanContainer.getBean(
				"multiple-1", MultiplePrototypesInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE,
				IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertNotNull(bean);
		MultiplePrototypesInSpringContextTestBean instance = bean.getBeanInstance();
		assertNotNull(instance);
		assertEquals("multiple-1", instance.getName());
		assertSame(applicationContext, instance.getApplicationContext());

		ContainedBean<MultiplePrototypesInSpringContextTestBean> bean2 = beanContainer.getBean(
				"multiple-1", MultiplePrototypesInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE,
				IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertNotNull(bean2);
		MultiplePrototypesInSpringContextTestBean instance2 = bean2.getBeanInstance();
		assertNotNull(instance2);
		// Due to the lifecycle options, and because the bean has the "prototype" scope, we should not return the same instance
		assertNotSame(instance, instance2);
	}


	/**
	 * The lifecycle options mandated by the JPA spec and used as a default in Hibernate ORM.
	 */
	private static class JpaLifecycleOptions implements BeanContainer.LifecycleOptions {
		public static final JpaLifecycleOptions INSTANCE = new JpaLifecycleOptions();

		@Override
		public boolean canUseCachedReferences() {
			return true;
		}

		@Override
		public boolean useJpaCompliantCreation() {
			return true;
		}
	}

	/**
	 * The lifecycle options used by libraries integrating into Hibernate ORM
	 * and that want a behavior closer to Spring's native behavior,
	 * such as Hibernate Search.
	 */
	private static class NativeLifecycleOptions implements BeanContainer.LifecycleOptions {
		public static final NativeLifecycleOptions INSTANCE = new NativeLifecycleOptions();

		@Override
		public boolean canUseCachedReferences() {
			return false;
		}

		@Override
		public boolean useJpaCompliantCreation() {
			return false;
		}
	}

	private static class IneffectiveBeanInstanceProducer implements BeanInstanceProducer {
		public static final IneffectiveBeanInstanceProducer INSTANCE = new IneffectiveBeanInstanceProducer();

		@Override
		public <B> B produceBeanInstance(Class<B> aClass) {
			throw new UnsupportedOperationException("should not be called");
		}

		@Override
		public <B> B produceBeanInstance(String s, Class<B> aClass) {
			throw new UnsupportedOperationException("should not be called");
		}
	}
}
