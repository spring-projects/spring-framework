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

package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.util.ClassUtils;

/**
 * An AOT {@link BeanRegistrationAotProcessor} that detects the presence of
 * classes compiled with AspectJ and adds the related required field hints.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
class AspectJAdvisorBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	private static final String AJC_MAGIC = "ajc$";

	private static final boolean aspectjPresent = ClassUtils.isPresent("org.aspectj.lang.annotation.Pointcut",
			AspectJAdvisorBeanRegistrationAotProcessor.class.getClassLoader());


	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (aspectjPresent) {
			Class<?> beanClass = registeredBean.getBeanClass();
			if (compiledByAjc(beanClass)) {
				return new AspectJAdvisorContribution(beanClass);
			}
		}
		return null;
	}

	private static boolean compiledByAjc(Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {
			if (field.getName().startsWith(AJC_MAGIC)) {
				return true;
			}
		}
		return false;
	}


	private static class AspectJAdvisorContribution implements BeanRegistrationAotContribution {

		private final Class<?> beanClass;

		public AspectJAdvisorContribution(Class<?> beanClass) {
			this.beanClass = beanClass;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			generationContext.getRuntimeHints().reflection().registerType(this.beanClass, MemberCategory.ACCESS_DECLARED_FIELDS);
		}
	}

}
