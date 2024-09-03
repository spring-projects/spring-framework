/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.List;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
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

	private static final boolean aspectJPresent = ClassUtils.isPresent("org.aspectj.lang.annotation.Pointcut",
			AspectJBeanFactoryInitializationAotProcessor.class.getClassLoader());


	@Override
	@Nullable
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		if (aspectJPresent) {
			return AspectDelegate.processAheadOfTime(beanFactory);
		}
		return null;
	}


	/**
	 * Inner class to avoid a hard dependency on AspectJ at runtime.
	 */
	private static class AspectDelegate {

		@Nullable
		private static AspectContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
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
