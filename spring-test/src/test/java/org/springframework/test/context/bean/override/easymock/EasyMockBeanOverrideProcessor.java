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

package org.springframework.test.context.bean.override.easymock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideProcessor;
import org.springframework.util.StringUtils;

/**
 * {@link BeanOverrideProcessor} that provides support for {@link EasyMockBean @EasyMockBean}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class EasyMockBeanOverrideProcessor implements BeanOverrideProcessor {

	@Override
	public BeanOverrideHandler createHandler(Annotation annotation, Class<?> testClass, Field field) {
		EasyMockBean easyMockBean = (EasyMockBean) annotation;
		String beanName = (StringUtils.hasText(easyMockBean.name()) ? easyMockBean.name() : null);
		return new EasyMockBeanOverrideHandler(field, field.getType(), beanName, easyMockBean.mockType());
	}

}
