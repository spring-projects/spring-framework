/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.aot;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.KotlinDetector;

/**
 * AOT {@code BeanRegistrationAotProcessor} that adds additional hints
 * required by Kotlin reflection.
 *
 * @author Sebastien Deleuze
 * @since 6.0.4
 */
class KotlinReflectionBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		if (KotlinDetector.isKotlinType(beanClass)) {
			return new AotContribution(beanClass);
		}
		return null;
	}


	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Class<?> beanClass;

		public AotContribution(Class<?> beanClass) {
			this.beanClass = beanClass;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			registerHints(this.beanClass, generationContext.getRuntimeHints());
		}

		private void registerHints(Class<?> type, RuntimeHints runtimeHints) {
			if (KotlinDetector.isKotlinType(type)) {
				runtimeHints.reflection().registerType(type);
			}
			Class<?> superClass = type.getSuperclass();
			if (superClass != null) {
				registerHints(superClass, runtimeHints);
			}
			Class<?> enclosingClass = type.getEnclosingClass();
			if (enclosingClass != null) {
				runtimeHints.reflection().registerType(enclosingClass);
			}
		}
	}

}
