/*
 * Copyright 2002-2024 the original author or authors.
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

import jakarta.persistence.PersistenceException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationAdvisorTests.RepositoryInterface;
import org.springframework.dao.annotation.PersistenceExceptionTranslationAdvisorTests.RepositoryInterfaceImpl;
import org.springframework.dao.annotation.PersistenceExceptionTranslationAdvisorTests.StereotypedRepositoryInterfaceImpl;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class PersistenceExceptionTranslationPostProcessorTests {

	@Test
	void proxiesCorrectly() {
		GenericApplicationContext gac = new GenericApplicationContext();
		gac.registerBeanDefinition("translator",
				new RootBeanDefinition(PersistenceExceptionTranslationPostProcessor.class));
		gac.registerBeanDefinition("notProxied", new RootBeanDefinition(RepositoryInterfaceImpl.class));
		gac.registerBeanDefinition("proxied", new RootBeanDefinition(StereotypedRepositoryInterfaceImpl.class));
		gac.registerBeanDefinition("classProxied", new RootBeanDefinition(RepositoryWithoutInterface.class));
		gac.registerBeanDefinition("classProxiedAndAdvised",
				new RootBeanDefinition(RepositoryWithoutInterfaceAndOtherwiseAdvised.class));
		gac.registerBeanDefinition("myTranslator",
				new RootBeanDefinition(MyPersistenceExceptionTranslator.class));
		gac.registerBeanDefinition("proxyCreator",
				BeanDefinitionBuilder.rootBeanDefinition(AnnotationAwareAspectJAutoProxyCreator.class).
						addPropertyValue("order", 50).getBeanDefinition());
		gac.registerBeanDefinition("logger", new RootBeanDefinition(LogAllAspect.class));
		gac.refresh();

		RepositoryInterface shouldNotBeProxied = (RepositoryInterface) gac.getBean("notProxied");
		assertThat(AopUtils.isAopProxy(shouldNotBeProxied)).isFalse();
		RepositoryInterface shouldBeProxied = (RepositoryInterface) gac.getBean("proxied");
		assertThat(AopUtils.isAopProxy(shouldBeProxied)).isTrue();
		RepositoryWithoutInterface rwi = (RepositoryWithoutInterface) gac.getBean("classProxied");
		assertThat(AopUtils.isAopProxy(rwi)).isTrue();
		checkWillTranslateExceptions(rwi);

		Additional rwi2 = (Additional) gac.getBean("classProxiedAndAdvised");
		assertThat(AopUtils.isAopProxy(rwi2)).isTrue();
		rwi2.additionalMethod(false);
		checkWillTranslateExceptions(rwi2);
		assertThatExceptionOfType(DataAccessResourceFailureException.class).isThrownBy(() ->
				rwi2.additionalMethod(true))
			.withMessage("my failure");
	}

	protected void checkWillTranslateExceptions(Object o) {
		assertThat(o).isInstanceOf(Advised.class);
		assertThat(((Advised) o).getAdvisors()).anyMatch(
				PersistenceExceptionTranslationAdvisor.class::isInstance);
	}


	@Repository
	public static class RepositoryWithoutInterface {

		public void nameDoesntMatter() {
		}
	}


	public interface Additional {

		void additionalMethod(boolean fail);
	}


	public static class RepositoryWithoutInterfaceAndOtherwiseAdvised extends StereotypedRepositoryInterfaceImpl
			implements Additional {

		@Override
		public void additionalMethod(boolean fail) {
			if (fail) {
				throw new PersistenceException("my failure");
			}
		}
	}


	public static class MyPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

		@Override
		public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
			if (ex instanceof PersistenceException) {
				return new DataAccessResourceFailureException(ex.getMessage());
			}
			return null;
		}
	}


	@Aspect
	public static class LogAllAspect {

		@Before("execution(void *.additionalMethod(*))")
		public void log(JoinPoint jp) {
			// System.out.println("Before " + jp.getSignature().getName());
		}
	}

}
