/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.bean.override;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.DummyBean.DummyBeanOverrideProcessor;
import org.springframework.util.StringUtils;

/**
 * A dummy {@link BeanOverride} implementation that only handles {@link CharSequence}
 * and {@link Integer} and replaces them with {@code "overridden"} and {@code 42},
 * respectively.
 *
 * @author Stephane Nicoll
 */
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(DummyBeanOverrideProcessor.class)
@interface DummyBean {

	String beanName() default "";

	String contextName() default "";

	BeanOverrideStrategy strategy() default BeanOverrideStrategy.REPLACE;

	class DummyBeanOverrideProcessor implements BeanOverrideProcessor {

		@Override
		public BeanOverrideHandler createHandler(Annotation annotation, Class<?> testClass, Field field) {
			DummyBean dummyBean = (DummyBean) annotation;
			String beanName = (StringUtils.hasText(dummyBean.beanName()) ? dummyBean.beanName() : null);
			return new DummyBeanOverrideProcessor.DummyBeanOverrideHandler(field, field.getType(), beanName,
					dummyBean.contextName(), dummyBean.strategy());
		}

		// Bare bone, "dummy", implementation that should not override anything
		// other than createOverrideInstance().
		static class DummyBeanOverrideHandler extends BeanOverrideHandler {

			DummyBeanOverrideHandler(Field field, Class<?> typeToOverride, @Nullable String beanName,
					String contextName, BeanOverrideStrategy strategy) {

				super(field, ResolvableType.forClass(typeToOverride), beanName, contextName, strategy);
			}

			@Override
			protected Object createOverrideInstance(String beanName, @Nullable BeanDefinition existingBeanDefinition,
					@Nullable Object existingBeanInstance) {

				Class<?> beanType = getField().getType();
				if (CharSequence.class.isAssignableFrom(beanType)) {
					return "overridden";
				}
				else if (Integer.class.isAssignableFrom(beanType)) {
					return 42;
				}
				throw new IllegalStateException("Could not handle bean type " + beanType);
			}
		}
	}

}
