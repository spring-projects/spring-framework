/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link FactoryBean} for a JCache {@link javax.cache.CacheManager},
 * obtaining a pre-defined CacheManager by name through the standard
 * JCache {@link javax.cache.Caching} class.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see javax.cache.Caching#getCacheManager()
 * @see javax.cache.Caching#getCacheManager(String)
 */
public class JCacheManagerFactoryBean
		implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	private String cacheManagerName = Caching.DEFAULT_CACHE_MANAGER_NAME;

	private ClassLoader beanClassLoader;

	private CacheManager cacheManager;


	/**
	 * Specify the name of the desired CacheManager.
	 * Default is JCache's default.
	 * @see Caching#DEFAULT_CACHE_MANAGER_NAME
	 */
	public void setCacheManagerName(String cacheManagerName) {
		this.cacheManagerName = cacheManagerName;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void afterPropertiesSet() {
		this.cacheManager = (this.beanClassLoader != null ?
				Caching.getCacheManager(this.beanClassLoader, this.cacheManagerName) :
				Caching.getCacheManager(this.cacheManagerName));
	}


	public CacheManager getObject() {
		return this.cacheManager;
	}

	public Class<?> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	public boolean isSingleton() {
		return true;
	}


	public void destroy() {
		this.cacheManager.shutdown();
	}

}
