/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.easymock;

import java.lang.reflect.Field;

import org.easymock.EasyMock;
import org.easymock.MockType;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
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

		super(field, ResolvableType.forClass(typeToOverride), beanName, "", REPLACE_OR_CREATE);
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
