/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
