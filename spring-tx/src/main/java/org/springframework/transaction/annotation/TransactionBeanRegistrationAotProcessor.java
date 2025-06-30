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

package org.springframework.transaction.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * AOT {@code BeanRegistrationAotProcessor} that detects the presence of
 * {@link Transactional @Transactional} on annotated elements and creates
 * the required reflection hints.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see TransactionRuntimeHints
 */
class TransactionBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	private static final String JAKARTA_TRANSACTIONAL_CLASS_NAME = "jakarta.transaction.Transactional";


	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		if (isTransactional(beanClass)) {
			return new AotContribution(beanClass);
		}
		return null;
	}

	private boolean isTransactional(Class<?> beanClass) {
		Set<AnnotatedElement> elements = new LinkedHashSet<>();
		elements.add(beanClass);
		ReflectionUtils.doWithMethods(beanClass, elements::add);
		for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClass(beanClass)) {
			elements.add(interfaceClass);
			ReflectionUtils.doWithMethods(interfaceClass, elements::add);
		}
		return elements.stream().anyMatch(element -> {
			MergedAnnotations mergedAnnotations = MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
			return mergedAnnotations.isPresent(Transactional.class) || mergedAnnotations.isPresent(JAKARTA_TRANSACTIONAL_CLASS_NAME);
		});
	}


	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Class<?> beanClass;

		public AotContribution(Class<?> beanClass) {
			this.beanClass = beanClass;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			RuntimeHints runtimeHints = generationContext.getRuntimeHints();
			Class<?>[] proxyInterfaces = ClassUtils.getAllInterfacesForClass(this.beanClass);
			for (Class<?> proxyInterface : proxyInterfaces) {
				runtimeHints.reflection().registerType(proxyInterface, MemberCategory.INVOKE_DECLARED_METHODS);
			}
		}
	}

}
