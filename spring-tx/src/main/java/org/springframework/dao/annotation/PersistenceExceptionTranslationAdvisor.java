/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.dao.annotation;

import java.lang.annotation.Annotation;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Spring AOP exception translation aspect for use at Repository or DAO layer level.
 * Translates native persistence exceptions into Spring's DataAccessException hierarchy,
 * based on a given PersistenceExceptionTranslator.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.support.PersistenceExceptionTranslator
 */
public class PersistenceExceptionTranslationAdvisor extends AbstractPointcutAdvisor {

	private final PersistenceExceptionTranslationInterceptor advice;

	private final AnnotationMatchingPointcut pointcut;


	/**
	 * Create a new PersistenceExceptionTranslationAdvisor.
	 * @param persistenceExceptionTranslator the PersistenceExceptionTranslator to use
	 * @param repositoryAnnotationType the annotation type to check for
	 */
	public PersistenceExceptionTranslationAdvisor(
			PersistenceExceptionTranslator persistenceExceptionTranslator,
			Class<? extends Annotation> repositoryAnnotationType) {

		this.advice = new PersistenceExceptionTranslationInterceptor(persistenceExceptionTranslator);
		this.pointcut = new AnnotationMatchingPointcut(repositoryAnnotationType, true);
	}

	/**
	 * Create a new PersistenceExceptionTranslationAdvisor.
	 * @param beanFactory the ListableBeanFactory to obtaining all
	 * PersistenceExceptionTranslators from
	 * @param repositoryAnnotationType the annotation type to check for
	 */
	PersistenceExceptionTranslationAdvisor(
			ListableBeanFactory beanFactory, Class<? extends Annotation> repositoryAnnotationType) {

		this.advice = new PersistenceExceptionTranslationInterceptor(beanFactory);
		this.pointcut = new AnnotationMatchingPointcut(repositoryAnnotationType, true);
	}


	public Advice getAdvice() {
		return this.advice;
	}

	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
