/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractGenericPointcutAdvisor;

/**
 * Convenient class for annotation-matching method pointcuts that hold an Advice,
 * making them an Advisor.
 *
 * @author Laszlo Csontos
 * @since 4.3
 * @see AnnotationMatchingPointcut
 */
@SuppressWarnings("serial")
public class AnnotationMatchingMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

	private final AnnotationMatchingPointcut pointcut;


	public AnnotationMatchingMethodPointcutAdvisor(Class<? extends Annotation> methodAnnotationType) {
		pointcut = AnnotationMatchingPointcut.forMethodAnnotation(methodAnnotationType);
	}

	public AnnotationMatchingMethodPointcutAdvisor(Class<? extends Annotation> methodAnnotationType, Advice advice) {
		this(methodAnnotationType);
		setAdvice(advice);
	}


	@Override
	public Pointcut getPointcut() {
		return pointcut;
	}

}
