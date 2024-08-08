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

package org.springframework.context.aot;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.context.annotation.ReflectiveScan;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * AOT {@code BeanFactoryInitializationAotProcessor} that detects the presence
 * of {@link Reflective @Reflective} on annotated elements of all registered
 * beans and invokes the underlying {@link ReflectiveProcessor} implementations.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectiveProcessorBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	@Nullable
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		Class<?>[] beanClasses = Arrays.stream(beanFactory.getBeanDefinitionNames())
				.map(beanName -> RegisteredBean.of(beanFactory, beanName).getBeanClass())
				.toArray(Class<?>[]::new);
		String[] packagesToScan = findBasePackagesToScan(beanClasses);
		return new ReflectiveProcessorAotContributionBuilder().withClasses(beanClasses)
				.scan(beanFactory.getBeanClassLoader(), packagesToScan).build();
	}

	protected String[] findBasePackagesToScan(Class<?>[] beanClasses) {
		Set<String> basePackages = new LinkedHashSet<>();
		for (Class<?> beanClass : beanClasses) {
			ReflectiveScan reflectiveScan = AnnotatedElementUtils.getMergedAnnotation(beanClass, ReflectiveScan.class);
			if (reflectiveScan != null) {
				basePackages.addAll(extractBasePackages(reflectiveScan, beanClass));
			}
		}
		return basePackages.toArray(new String[0]);
	}

	private Set<String> extractBasePackages(ReflectiveScan annotation, Class<?> declaringClass) {
		Set<String> basePackages = new LinkedHashSet<>();
		Collections.addAll(basePackages, annotation.basePackages());
		for (Class<?> clazz : annotation.basePackageClasses()) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}
		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(declaringClass));
		}
		return basePackages;
	}

}
