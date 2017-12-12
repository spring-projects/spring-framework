/*
 * Copyright 2010-2017 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.util.Optional;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * Proxy factory bean for simplified declarative caching handling.
 * This is a convenient alternative to a standard AOP
 * {@link org.springframework.aop.framework.ProxyFactoryBean}
 * with a separate {@link CacheInterceptor} definition.
 *
 * <p>This class is designed to facilitate declarative cache demarcation: namely, wrapping
 * a singleton target object with a caching proxy, proxying all the interfaces that the
 * target implements. Exists primarily for third-party framework integration.
 * <strong>Users should favor the {@code cache:} XML namespace
 * {@link org.springframework.cache.annotation.Cacheable @Cacheable} annotation.</strong>
 * See the <a href="http://bit.ly/p9rIvx">declarative annotation-based caching</a> section
 * of the Spring reference documentation for more information.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author John Blum
 * @since 3.1
 * @see org.springframework.aop.framework.AbstractSingletonProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.SmartInitializingSingleton
 * @see org.springframework.cache.interceptor.CacheInterceptor
 */
@SuppressWarnings("serial")
public class CacheProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware, SmartInitializingSingleton {

	// SimpleKeyGenerator is stateless and therefore Thread-safe; sharing instance using a static constant
	protected static final KeyGenerator DEFAULT_KEY_GENERATOR = new SimpleKeyGenerator();

	private final CacheInterceptor cachingInterceptor = new CacheInterceptor();

	private Pointcut pointcut = Pointcut.TRUE;

	/**
	 * Returns a reference to the {@link CacheInterceptor} configured by
	 * this {@link CacheProxyFactoryBean FactoryBean}.
	 *
	 * @return a reference to the {@link CacheInterceptor}.
	 * @see org.springframework.cache.interceptor.CacheInterceptor
	 */
	protected CacheInterceptor getCachingInterceptor() {
		return this.cachingInterceptor;
	}

	/**
	 * Sets a reference to the owning {@link BeanFactory}.
	 *
	 * @param beanFactory owning BeanFactory (never {@code null}).
	 * The bean can immediately call methods on the factory.
	 * @throws BeansException in case of initialization errors.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		CacheInterceptor cacheInterceptor = getCachingInterceptor();

		cacheInterceptor.setBeanFactory(beanFactory);
		cacheInterceptor.setCacheManager(beanFactory.getBean("cacheManager", CacheManager.class));
	}

	/**
	 * Set the sources used to find cache operations.
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		getCachingInterceptor().setCacheOperationSources(cacheOperationSources);
	}

	/**
	 * Sets a reference to the {@link CacheResolver} used to resolve {@link Cache Caches} identified
	 * in the application's intercepted method invocations involving caching.
	 *
	 * @param cacheResolver {@link CacheResolver} used to determine the {@link Cache Cache or Caches}
	 * used during the intercepted method invocation.
	 * @see org.springframework.cache.interceptor.CacheResolver
	 */
	public void setCacheResolver(@Nullable CacheResolver cacheResolver) {
		getCachingInterceptor().setCacheResolver(cacheResolver);
	}

	public void setKeyGenerator(@Nullable KeyGenerator keyGenerator) {
		getCachingInterceptor().setKeyGenerator(Optional.ofNullable(keyGenerator)
			.orElse(DEFAULT_KEY_GENERATOR));
	}

	/**
	 * Set a pointcut, i.e a bean that can cause conditional invocation
	 * of the CacheInterceptor depending on method and attributes passed.
	 * Note: Additional interceptors are always invoked.
	 * @see #setPreInterceptors
	 * @see #setPostInterceptors
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	@Override
	public void afterSingletonsInstantiated() {
		getCachingInterceptor().afterSingletonsInstantiated();
	}

	@Override
	protected Object createMainInterceptor() {

		CacheInterceptor cacheInterceptor = getCachingInterceptor();

		cacheInterceptor.afterPropertiesSet();

		return new DefaultPointcutAdvisor(this.pointcut, cacheInterceptor);
	}

}
