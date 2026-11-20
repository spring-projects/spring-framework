/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} implementation responsible for registering
 * hints for AOP advices.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0.11
 */
class AspectJBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private static final boolean ASPECTJ_PRESENT = ClassUtils.isPresent("org.aspectj.lang.annotation.Pointcut",
			AspectJBeanFactoryInitializationAotProcessor.class.getClassLoader());


	@Override
	public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		if (ASPECTJ_PRESENT) {
			return AspectDelegate.processAheadOfTime(beanFactory);
		}
		return null;
	}


	/**
	 * Inner class to avoid a hard dependency on AspectJ at runtime.
	 */
	private static class AspectDelegate {

		private static @Nullable AspectContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
			BeanFactoryAspectJAdvisorsBuilder builder = new BeanFactoryAspectJAdvisorsBuilder(beanFactory);
			List<Advisor> advisors = builder.buildAspectJAdvisors();
			return (advisors.isEmpty() ? null : new AspectContribution(advisors));
		}
	}


	private static class AspectContribution implements BeanFactoryInitializationAotContribution {

		private final List<Advisor> advisors;

		public AspectContribution(List<Advisor> advisors) {
			this.advisors = advisors;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
			for (Advisor advisor : this.advisors) {
				if (advisor.getAdvice() instanceof AbstractAspectJAdvice aspectJAdvice) {
					reflectionHints.registerMethod(aspectJAdvice.getAspectJAdviceMethod(), ExecutableMode.INVOKE);
				}
			}
		}
	}

}
