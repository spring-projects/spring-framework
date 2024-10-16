/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;

/**
 * {@code TestExecutionListener} that resets any mock beans that have been marked
 * with a {@link MockReset}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoBean @MockitoBean
 * @see MockitoSpyBean @MockitoSpyBean
 */
public class MockitoResetTestExecutionListener extends AbstractTestExecutionListener {

	static final boolean mockitoPresent = ClassUtils.isPresent("org.mockito.Mockito",
			MockitoResetTestExecutionListener.class.getClassLoader());

	private static final String SPRING_MOCKITO_PACKAGE = "org.springframework.test.context.bean.override.mockito";

	private static final Predicate<MergedAnnotation<?>> isMockitoAnnotation = mergedAnnotation -> {
			String packageName = mergedAnnotation.getType().getPackageName();
			return packageName.startsWith(SPRING_MOCKITO_PACKAGE);
		};

	/**
	 * Executes before {@link org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener}.
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) {
		if (mockitoPresent && hasMockitoAnnotations(testContext)) {
			resetMocks(testContext.getApplicationContext(), MockReset.BEFORE);
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) {
		if (mockitoPresent && hasMockitoAnnotations(testContext)) {
			resetMocks(testContext.getApplicationContext(), MockReset.AFTER);
		}
	}


	private void resetMocks(ApplicationContext applicationContext, MockReset reset) {
		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			resetMocks(configurableContext, reset);
		}
	}

	private void resetMocks(ConfigurableApplicationContext applicationContext, MockReset reset) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		String[] beanNames = beanFactory.getBeanDefinitionNames();
		Set<String> instantiatedSingletons = new HashSet<>(Arrays.asList(beanFactory.getSingletonNames()));
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (beanDefinition.isSingleton() && instantiatedSingletons.contains(beanName)) {
				Object bean = getBean(beanFactory, beanName);
				if (bean != null && reset == MockReset.get(bean)) {
					Mockito.reset(bean);
				}
			}
		}
		try {
			beanFactory.getBean(MockitoBeans.class).resetAll();
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Continue
		}
		if (applicationContext.getParent() != null) {
			resetMocks(applicationContext.getParent(), reset);
		}
	}

	@Nullable
	private static Object getBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
		try {
			if (isStandardBeanOrSingletonFactoryBean(beanFactory, beanName)) {
				return beanFactory.getBean(beanName);
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return beanFactory.getSingleton(beanName);
	}

	private static boolean isStandardBeanOrSingletonFactoryBean(BeanFactory beanFactory, String beanName) {
		String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
		if (beanFactory.containsBean(factoryBeanName)) {
			FactoryBean<?> factoryBean = (FactoryBean<?>) beanFactory.getBean(factoryBeanName);
			return factoryBean.isSingleton();
		}
		return true;
	}

	/**
	 * Determine if the test class for the supplied {@linkplain TestContext
	 * test context} uses any of the annotations in this package (such as
	 * {@link MockitoBean @MockitoBean}).
	 */
	static boolean hasMockitoAnnotations(TestContext testContext) {
		return hasMockitoAnnotations(testContext.getTestClass());
	}

	/**
	 * Determine if Mockito annotations are declared on the supplied class, on an
	 * interface it implements, on a superclass, or on an enclosing class or
	 * whether a field in any such class is annotated with a Mockito annotation.
	 */
	private static boolean hasMockitoAnnotations(Class<?> clazz) {
		// Declared on the class?
		if (MergedAnnotations.from(clazz, MergedAnnotations.SearchStrategy.DIRECT).stream().anyMatch(isMockitoAnnotation)) {
			return true;
		}

		// Declared on a field?
		for (Field field : clazz.getDeclaredFields()) {
			if (MergedAnnotations.from(field, MergedAnnotations.SearchStrategy.DIRECT).stream().anyMatch(isMockitoAnnotation)) {
				return true;
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			if (hasMockitoAnnotations(ifc)) {
				return true;
			}
		}

		// Declared on a superclass?
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null & superclass != Object.class) {
			if (hasMockitoAnnotations(superclass)) {
				return true;
			}
		}

		// Declared on an enclosing class of an inner class?
		if (TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
			if (hasMockitoAnnotations(clazz.getEnclosingClass())) {
				return true;
			}
		}

		return false;
	}

}
