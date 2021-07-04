/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.dao.annotation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.stereotype.Repository;

/**
 * Tests for standalone usage of a PersistenceExceptionTranslationInterceptor, as explicit advice bean in a BeanFactory
 * rather than applied as part of a PersistenceExceptionTranslationAdvisor.
 *
 * @author Juergen Hoeller
 */
public class PersistenceExceptionTranslationInterceptorTests extends PersistenceExceptionTranslationAdvisorTests {

	@Override
	protected void addPersistenceExceptionTranslation(ProxyFactory pf, PersistenceExceptionTranslator pet) {
		if (AnnotationUtils.findAnnotation(pf.getTargetClass(), Repository.class) != null) {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			bf.registerBeanDefinition("peti", new RootBeanDefinition(PersistenceExceptionTranslationInterceptor.class));
			bf.registerSingleton("pet", pet);
			pf.addAdvice((PersistenceExceptionTranslationInterceptor) bf.getBean("peti"));
		}
	}

}
