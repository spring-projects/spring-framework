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

import java.lang.reflect.Field;

import org.easymock.EasyMock;
import org.easymock.MockType;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideHandler;

import static org.springframework.test.context.bean.override.BeanOverrideStrategy.REPLACE_OR_CREATE;

/**
 * {@link BeanOverrideHandler} that provides support for {@link EasyMockBean @EasyMockBean}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class EasyMockBeanOverrideHandler extends BeanOverrideHandler {

	private final MockType mockType;


	EasyMockBeanOverrideHandler(Field field, Class<?> typeToOverride, @Nullable String beanName,
			MockType mockType) {

		super(field, ResolvableType.forClass(typeToOverride), beanName, REPLACE_OR_CREATE);
		this.mockType = mockType;
	}


	@Override
	protected Object createOverrideInstance(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance) {

		Class<?> typeToMock = getBeanType().getRawClass();
		return EasyMock.mock(beanName, this.mockType, typeToMock);
	}

	@Override
	protected void trackOverrideInstance(Object mock, SingletonBeanRegistry singletonBeanRegistry) {
		getEasyMockBeans(singletonBeanRegistry).add(mock);
	}

	private static EasyMockBeans getEasyMockBeans(SingletonBeanRegistry singletonBeanRegistry) {
		String beanName = EasyMockBeans.class.getName();
		EasyMockBeans easyMockBeans = null;
		if (singletonBeanRegistry.containsSingleton(beanName)) {
			easyMockBeans = (EasyMockBeans) singletonBeanRegistry.getSingleton(beanName);
		}
		if (easyMockBeans == null) {
			easyMockBeans = new EasyMockBeans();
			singletonBeanRegistry.registerSingleton(beanName, easyMockBeans);
		}
		return easyMockBeans;
	}

}
