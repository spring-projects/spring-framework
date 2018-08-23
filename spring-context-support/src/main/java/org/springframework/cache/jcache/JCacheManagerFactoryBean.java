/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.jcache;

import java.net.URI;
import java.util.Properties;
import javax.cache.CacheManager;
import javax.cache.Caching;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean} for a JCache {@link CacheManager javax.cache.CacheManager},
 * obtaining a pre-defined {@code CacheManager} by name through the standard
 * JCache {@link Caching javax.cache.Caching} class.
 *
 * <p>Note: This class has been updated for JCache 1.0, as of Spring 4.0.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see javax.cache.Caching#getCachingProvider()
 * @see javax.cache.spi.CachingProvider#getCacheManager()
 */
public class JCacheManagerFactoryBean
		implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	@Nullable
	private URI cacheManagerUri;

	@Nullable
	private Properties cacheManagerProperties;

	@Nullable
	private ClassLoader beanClassLoader;

	@Nullable
	private CacheManager cacheManager;


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
	@Nullable
	public CacheManager getObject() {
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
