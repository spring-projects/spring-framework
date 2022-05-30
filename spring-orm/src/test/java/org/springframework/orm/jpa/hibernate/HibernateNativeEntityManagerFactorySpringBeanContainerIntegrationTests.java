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

package org.springframework.orm.jpa.hibernate;

import jakarta.persistence.AttributeConverter;
import org.hibernate.SessionFactory;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.hibernate.beans.BeanSource;
import org.springframework.orm.jpa.hibernate.beans.MultiplePrototypesInSpringContextTestBean;
import org.springframework.orm.jpa.hibernate.beans.NoDefinitionInSpringContextTestBean;
import org.springframework.orm.jpa.hibernate.beans.SinglePrototypeInSpringContextTestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Hibernate-specific SpringBeanContainer integration tests.
 *
 * @author Yoann Rodiere
 * @author Juergen Hoeller
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
		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		ServiceRegistry serviceRegistry = sessionFactory.getSessionFactoryOptions().getServiceRegistry();
		return serviceRegistry.requireService(ManagedBeanRegistry.class);
	}

	private BeanContainer getBeanContainer() {
		return getManagedBeanRegistry().getBeanContainer();
	}


	@Test
	public void testCanRetrieveBeanByTypeWithJpaCompliantOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();

		ContainedBean<SinglePrototypeInSpringContextTestBean> bean = beanContainer.getBean(
				SinglePrototypeInSpringContextTestBean.class,
				JpaLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertThat(bean).isNotNull();
		SinglePrototypeInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getApplicationContext()).isSameAs(applicationContext);
	}

	@Test
	public void testCanRetrieveBeanByNameWithJpaCompliantOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();

		ContainedBean<MultiplePrototypesInSpringContextTestBean> bean = beanContainer.getBean(
				"multiple-1", MultiplePrototypesInSpringContextTestBean.class,
				JpaLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertThat(bean).isNotNull();
		MultiplePrototypesInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getName()).isEqualTo("multiple-1");
		assertThat(instance.getApplicationContext()).isSameAs(applicationContext);
	}

	@Test
	public void testCanRetrieveBeanByTypeWithNativeOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();

		ContainedBean<SinglePrototypeInSpringContextTestBean> bean = beanContainer.getBean(
				SinglePrototypeInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertThat(bean).isNotNull();
		SinglePrototypeInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getName()).isEqualTo("single");
		assertThat(instance.getApplicationContext()).isSameAs(applicationContext);

		ContainedBean<SinglePrototypeInSpringContextTestBean> bean2 = beanContainer.getBean(
				SinglePrototypeInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertThat(bean2).isNotNull();
		SinglePrototypeInSpringContextTestBean instance2 = bean2.getBeanInstance();
		assertThat(instance2).isNotNull();
		// Due to the lifecycle options, and because the bean has the "prototype" scope, we should not return the same instance
		assertThat(instance2).isNotSameAs(instance);
	}

	@Test
	public void testCanRetrieveBeanByNameWithNativeOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();

		ContainedBean<MultiplePrototypesInSpringContextTestBean> bean = beanContainer.getBean(
				"multiple-1", MultiplePrototypesInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertThat(bean).isNotNull();
		MultiplePrototypesInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getName()).isEqualTo("multiple-1");
		assertThat(instance.getApplicationContext()).isSameAs(applicationContext);

		ContainedBean<MultiplePrototypesInSpringContextTestBean> bean2 = beanContainer.getBean(
				"multiple-1", MultiplePrototypesInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
		);

		assertThat(bean2).isNotNull();
		MultiplePrototypesInSpringContextTestBean instance2 = bean2.getBeanInstance();
		assertThat(instance2).isNotNull();
		// Due to the lifecycle options, and because the bean has the "prototype" scope, we should not return the same instance
		assertThat(instance2).isNotSameAs(instance);
	}

	@Test
	public void testCanRetrieveFallbackBeanByTypeWithJpaCompliantOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();
		NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

		ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
				NoDefinitionInSpringContextTestBean.class,
				JpaLifecycleOptions.INSTANCE, fallbackProducer
		);

		assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(1);
		assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(0);

		assertThat(bean).isNotNull();
		NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
		assertThat(instance.getApplicationContext()).isNull();
	}

	@Test
	public void testCanRetrieveFallbackBeanByNameWithJpaCompliantOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();
		NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

		ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
				"some name", NoDefinitionInSpringContextTestBean.class,
				JpaLifecycleOptions.INSTANCE, fallbackProducer
		);

		assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(0);
		assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(1);

		assertThat(bean).isNotNull();
		NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
		assertThat(instance.getName()).isEqualTo("some name");
		assertThat(instance.getApplicationContext()).isNull();
	}

	@Test
	public void testCanRetrieveFallbackBeanByTypeWithNativeOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();
		NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

		ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
				NoDefinitionInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE, fallbackProducer
		);

		assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(1);
		assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(0);

		assertThat(bean).isNotNull();
		NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
		assertThat(instance.getApplicationContext()).isNull();
	}

	@Test
	public void testCanRetrieveFallbackBeanByNameWithNativeOptions() {
		BeanContainer beanContainer = getBeanContainer();
		assertThat(beanContainer).isNotNull();
		NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

		ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
				"some name", NoDefinitionInSpringContextTestBean.class,
				NativeLifecycleOptions.INSTANCE, fallbackProducer
		);

		assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(0);
		assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(1);

		assertThat(bean).isNotNull();
		NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
		assertThat(instance).isNotNull();
		assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
		assertThat(instance.getName()).isEqualTo("some name");
		assertThat(instance.getApplicationContext()).isNull();
	}

	@Test
	public void testFallbackExceptionInCaseOfNoSpringBeanFound() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
			getBeanContainer().getBean(NoDefinitionInSpringContextTestBean.class,
					NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
			));
	}

	@Test
	public void testOriginalExceptionInCaseOfFallbackProducerFailure() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
			getBeanContainer().getBean(AttributeConverter.class,
					NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
			));
	}

	@Test
	public void testFallbackExceptionInCaseOfNoSpringBeanFoundByName() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
			getBeanContainer().getBean("some name", NoDefinitionInSpringContextTestBean.class,
					NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
			));
	}

	@Test
	public void testOriginalExceptionInCaseOfFallbackProducerFailureByName() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
			getBeanContainer().getBean("invalid", AttributeConverter.class,
					NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
			));
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


	private static class NoDefinitionInSpringContextTestBeanInstanceProducer implements BeanInstanceProducer {

		private int unnamedInstantiationCount = 0;

		private int namedInstantiationCount = 0;

		@Override
		public <B> B produceBeanInstance(Class<B> beanType) {
			try {
				++unnamedInstantiationCount;
				/*
				 * We only expect to ever be asked to instantiate this class, so we just cut corners here.
				 * A real-world implementation would obviously be different.
				 */
				NoDefinitionInSpringContextTestBean instance = new NoDefinitionInSpringContextTestBean(null, BeanSource.FALLBACK);
				return beanType.cast( instance );
			}
			catch (RuntimeException e) {
				throw new AssertionError( "Unexpected error instantiating a bean by type using reflection", e );
			}
		}

		@Override
		public <B> B produceBeanInstance(String name, Class<B> beanType) {
			try {
				++namedInstantiationCount;
				/*
				 * We only expect to ever be asked to instantiate this class, so we just cut corners here.
				 * A real-world implementation would obviously be different.
				 */
				NoDefinitionInSpringContextTestBean instance = new NoDefinitionInSpringContextTestBean(name, BeanSource.FALLBACK);
				return beanType.cast( instance );
			}
			catch (RuntimeException e) {
				throw new AssertionError( "Unexpected error instantiating a bean by name using reflection", e );
			}
		}

		private int currentUnnamedInstantiationCount() {
			return unnamedInstantiationCount;
		}

		private int currentNamedInstantiationCount() {
			return namedInstantiationCount;
		}
	}

}
