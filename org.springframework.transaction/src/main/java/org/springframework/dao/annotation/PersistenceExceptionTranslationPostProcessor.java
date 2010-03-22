/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Bean post-processor that automatically applies persistence exception
 * translation to any bean that carries the
 * {@link org.springframework.stereotype.Repository} annotation,
 * adding a corresponding {@link PersistenceExceptionTranslationAdvisor}
 * to the exposed proxy (either an existing AOP proxy or a newly generated
 * proxy that implements all of the target's interfaces).
 *
 * <p>Translates native resource exceptions to Spring's
 * {@link org.springframework.dao.DataAccessException} hierarchy.
 * Autodetects beans that implement the
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator}
 * interface, which are subsequently asked to translate candidate exceptions.
 *
 * <p>All of Spring's applicable resource factories implement the
 * <code>PersistenceExceptionTranslator</code> interface out of the box.
 * As a consequence, all that is usually needed to enable automatic exception
 * translation is marking all affected beans (such as DAOs) with the
 * <code>Repository</code> annotation, along with defining this post-processor
 * as bean in the application context.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see PersistenceExceptionTranslationAdvisor
 * @see org.springframework.stereotype.Repository
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.support.PersistenceExceptionTranslator
 */
public class PersistenceExceptionTranslationPostProcessor extends ProxyConfig
		implements BeanPostProcessor, BeanClassLoaderAware, BeanFactoryAware, Ordered {

	private Class<? extends Annotation> repositoryAnnotationType = Repository.class;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private PersistenceExceptionTranslationAdvisor persistenceExceptionTranslationAdvisor;


	/**
	 * Set the 'repository' annotation type.
	 * The default repository annotation type is the {@link Repository} annotation.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a class has a
	 * repository role.
	 * @param repositoryAnnotationType the desired annotation type
	 */
	public void setRepositoryAnnotationType(Class<? extends Annotation> repositoryAnnotationType) {
		Assert.notNull(repositoryAnnotationType, "'requiredAnnotationType' must not be null");
		this.repositoryAnnotationType = repositoryAnnotationType;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalArgumentException(
					"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
		}
		this.persistenceExceptionTranslationAdvisor = new PersistenceExceptionTranslationAdvisor(
				(ListableBeanFactory) beanFactory, this.repositoryAnnotationType);
	}

	public int getOrder() {
		// This should run after all other post-processors, so that it can just add
		// an advisor to existing proxies rather than double-proxy.
		return LOWEST_PRECEDENCE;
	}


	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AopInfrastructureBean) {
			// Ignore AOP infrastructure such as scoped proxies.
			return bean;
		}
		Class targetClass = AopUtils.getTargetClass(bean);
		if (AopUtils.canApply(this.persistenceExceptionTranslationAdvisor, targetClass)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(this.persistenceExceptionTranslationAdvisor);
				return bean;
			}
			else {
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				// Copy our properties (proxyTargetClass etc) inherited from ProxyConfig.
				proxyFactory.copyFrom(this);
				proxyFactory.addAdvisor(this.persistenceExceptionTranslationAdvisor);
				return proxyFactory.getProxy(this.beanClassLoader);
			}
		}
		else {
			// This is not a repository.
			return bean;
		}
	}

}
