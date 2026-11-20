/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache;

import java.net.URI;
import java.util.Properties;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link FactoryBean} for a JCache {@link CacheManager javax.cache.CacheManager},
 * obtaining a pre-defined {@code CacheManager} by name through the standard
 * JCache {@link Caching javax.cache.Caching} class.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see javax.cache.Caching#getCachingProvider()
 * @see javax.cache.spi.CachingProvider#getCacheManager()
 */
public class JCacheManagerFactoryBean
		implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	private @Nullable URI cacheManagerUri;

	private @Nullable Properties cacheManagerProperties;

	private @Nullable ClassLoader beanClassLoader;

	private @Nullable CacheManager cacheManager;


	/**
	 * Specify the URI for the desired {@code CacheManager}.
	 * <p>Default is {@code null} (i.e. JCache's default).
	 */
	public void setCacheManagerUri(@Nullable URI cacheManagerUri) {
		this.cacheManagerUri = cacheManagerUri;
	}

	/**
	 * Specify properties for the to-be-created {@code CacheManager}.
	 * <p>Default is {@code null} (i.e. no special properties to apply).
	 * @see javax.cache.spi.CachingProvider#getCacheManager(URI, ClassLoader, Properties)
	 */
	public void setCacheManagerProperties(@Nullable Properties cacheManagerProperties) {
		this.cacheManagerProperties = cacheManagerProperties;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		this.cacheManager = Caching.getCachingProvider().getCacheManager(
				this.cacheManagerUri, this.beanClassLoader, this.cacheManagerProperties);
	}


	@Override
	public @Nullable CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.cacheManager != null) {
			this.cacheManager.close();
		}
	}

}
