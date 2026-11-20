/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	private static final boolean ASPECTJ_PRESENT = ClassUtils.isPresent("org.aspectj.lang.annotation.Pointcut",
			AspectJAdvisorBeanRegistrationAotProcessor.class.getClassLoader());


	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (ASPECTJ_PRESENT) {
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
