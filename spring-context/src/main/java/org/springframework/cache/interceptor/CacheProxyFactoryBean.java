/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.CacheManager;

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
 * See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache-annotations">declarative annotation-based caching</a>
 * section of the Spring reference documentation for more information.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see CacheInterceptor
 */
@SuppressWarnings("serial")
public class CacheProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware, SmartInitializingSingleton {

	private final CacheInterceptor cacheInterceptor = new CacheInterceptor();

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * Set one or more sources to find cache operations.
	 * @see CacheInterceptor#setCacheOperationSources
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		this.cacheInterceptor.setCacheOperationSources(cacheOperationSources);
	}

	/**
	 * Set the default {@link KeyGenerator} that this cache aspect should delegate to
	 * if no specific key generator has been set for the operation.
	 * <p>The default is a {@link SimpleKeyGenerator}.
	 * @since 5.0.3
	 * @see CacheInterceptor#setKeyGenerator
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.cacheInterceptor.setKeyGenerator(keyGenerator);
	}

	/**
	 * Set the default {@link CacheResolver} that this cache aspect should delegate
	 * to if no specific cache resolver has been set for the operation.
	 * <p>The default resolver resolves the caches against their names and the
	 * default cache manager.
	 * @since 5.0.3
	 * @see CacheInterceptor#setCacheResolver
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.cacheInterceptor.setCacheResolver(cacheResolver);
	}

	/**
	 * Set the {@link CacheManager} to use to create a default {@link CacheResolver}.
	 * Replace the current {@link CacheResolver}, if any.
	 * @since 5.0.3
	 * @see CacheInterceptor#setCacheManager
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheInterceptor.setCacheManager(cacheManager);
	}

	/**
	 * Set a pointcut, i.e. a bean that triggers conditional invocation of the
	 * {@link CacheInterceptor} depending on the method and attributes passed.
	 * <p>Note: Additional interceptors are always invoked.
	 * @see #setPreInterceptors
	 * @see #setPostInterceptors
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.cacheInterceptor.setBeanFactory(beanFactory);
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.cacheInterceptor.afterSingletonsInstantiated();
	}


	@Override
	protected Object createMainInterceptor() {
		this.cacheInterceptor.afterPropertiesSet();
		return new DefaultPointcutAdvisor(this.pointcut, this.cacheInterceptor);
	}

}
