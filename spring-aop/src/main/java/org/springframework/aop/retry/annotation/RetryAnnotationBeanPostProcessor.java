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

package org.springframework.aop.retry.annotation;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

/**
 * A convenient {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} that applies {@link RetryAnnotationInterceptor}
 * to all bean methods annotated with {@link Retryable} annotations.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
@SuppressWarnings("serial")
public class RetryAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	public RetryAnnotationBeanPostProcessor() {
		setBeforeExistingAdvisors(true);

		Pointcut cpc = new AnnotationMatchingPointcut(Retryable.class, true);
		Pointcut mpc = new AnnotationMatchingPointcut(null, Retryable.class, true);
		this.advisor = new DefaultPointcutAdvisor(
				new ComposablePointcut(cpc).union(mpc),
				new RetryAnnotationInterceptor());
	}

}
