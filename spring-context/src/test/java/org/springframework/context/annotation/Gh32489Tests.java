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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for gh-32489
 *
 * @author Stephane Nicoll
 */
public class Gh32489Tests {

	@Test
	void resolveFactoryBeansWithWildcard() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.register(SimpleRepositoryFactoriesBeanHolder.class);
			context.refresh();
			assertThat(context.getBean(SimpleRepositoryFactoriesBeanHolder.class).repositoryFactoryies)
					.containsOnly(context.getBean("&repositoryFactoryBean", SimpleRepositoryFactoryBean.class));
		}
	}

	@Test
	void resolveFactoryBeansParentInterfaceWithWildcard() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.register(RepositoryFactoriesInformationHolder.class);
			context.refresh();
			assertThat(context.getBean(RepositoryFactoriesInformationHolder.class).repositoryFactoresInformation)
					.containsOnly(context.getBean("&repositoryFactoryBean", SimpleRepositoryFactoryBean.class));
		}
	}

	@Test
	void resolveFactoryBeanWithMatchingGenerics() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.register(RepositoryFactoryHolder.class);
			context.refresh();
			assertThat(context.getBean(RepositoryFactoryHolder.class).repositoryFactory)
					.isEqualTo(context.getBean("&repositoryFactoryBean"));
		}
	}

	@Test
	void provideFactoryBeanWithMatchingGenerics() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.refresh();
			ResolvableType requiredType = ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
					EmployeeRepository.class, Long.class);
			assertThat(context.getBeanProvider(requiredType)).containsOnly(context.getBean("&repositoryFactoryBean"));
		}
	}

	@Test
	void provideFactoryBeanWithFirstNonMatchingGenerics() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.refresh();
			ResolvableType requiredType = ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
					TestBean.class, Long.class);
			assertThat(context.getBeanProvider(requiredType)).hasSize(0);
		}
	}

	@Test
	void provideFactoryBeanWithSecondNonMatchingGenerics() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.refresh();
			ResolvableType requiredType = ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
					EmployeeRepository.class, String.class);
			assertThat(context.getBeanProvider(requiredType)).hasSize(0);
		}
	}

	@Test
	void provideFactoryBeanTargetTypeWithMatchingGenerics() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.refresh();
			ResolvableType requiredType = ResolvableType.forClassWithGenerics(Repository.class,
					Employee.class, Long.class);
			assertThat(context.getBeanProvider(requiredType)).
					containsOnly(context.getBean("repositoryFactoryBean"));
		}
	}

	@Test
	void provideFactoryBeanTargetTypeWithFirstNonMatchingGenerics() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.refresh();
			ResolvableType requiredType = ResolvableType.forClassWithGenerics(Repository.class,
					TestBean.class, Long.class);
			assertThat(context.getBeanProvider(requiredType)).hasSize(0);
		}
	}

	@Test
	void provideFactoryBeanTargetTypeWithSecondNonMatchingGenerics() {
		try (AnnotationConfigApplicationContext context = prepareContext()) {
			context.refresh();
			ResolvableType requiredType = ResolvableType.forClassWithGenerics(Repository.class,
					Employee.class, String.class);
			assertThat(context.getBeanProvider(requiredType)).hasSize(0);
		}
	}

	private AnnotationConfigApplicationContext prepareContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		RootBeanDefinition rbd = new RootBeanDefinition(SimpleRepositoryFactoryBean.class);
		rbd.setTargetType(ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
				EmployeeRepository.class, Long.class));
		rbd.getConstructorArgumentValues().addIndexedArgumentValue(0, EmployeeRepository.class);
		context.registerBeanDefinition("repositoryFactoryBean", rbd);
		return context;
	}


	static class SimpleRepositoryFactoriesBeanHolder {

		@Autowired
		List<SimpleRepositoryFactoryBean<?, ?>> repositoryFactoryies;
	}

	static class RepositoryFactoriesInformationHolder {

		@Autowired
		List<RepositoryFactoryInformation<?, ?>> repositoryFactoresInformation;
	}

	static class RepositoryFactoryHolder {

		@Autowired
		SimpleRepositoryFactoryBean<EmployeeRepository, Long> repositoryFactory;
	}

	static class SimpleRepositoryFactoryBean<T, ID> extends RepositoryFactoryBeanSupport<T, ID> {

		private final Class<? extends T> repositoryType;

		public SimpleRepositoryFactoryBean(Class<? extends T> repositoryType) {
			this.repositoryType = repositoryType;
		}

		@Override
		public T getObject() throws Exception {
			return BeanUtils.instantiateClass(this.repositoryType);
		}

		@Override
		public Class<?> getObjectType() {
			return this.repositoryType;
		}
	}

	abstract static class RepositoryFactoryBeanSupport<T, ID> implements FactoryBean<T>, RepositoryFactoryInformation<T, ID> {
	}

	interface RepositoryFactoryInformation<T, ID> {
	}

	interface Repository<T, ID> {}

	static class EmployeeRepository implements Repository<Employee, Long> {}

	record Employee(Long id, String name) {}

}
