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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link CacheManager} implementation that gets {@link Cache} objects that are registered
 * in the {@link ApplicationContext} and resolved by using the bean name.
 *
 * @author Phill Escott
 * @since 5.0
 */
public class BeanAwareCacheManager implements CacheManager, InitializingBean, ApplicationContextAware {

	private Map<String, Cache> cacheMap;

	private ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.cacheMap = this.applicationContext.getBeansOfType(Cache.class);
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * This implementation returns a {@link Cache} implementation that has been registered
	 * in the {@link ApplicationContext}.
	 * 
	 * @name Name of the bean registered in the {@link ApplicationContext} not the name of
	 *       the Cache itself.
	 */
	@Override
	public Cache getCache(final String name) {
		return cacheMap.get(name);
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(cacheMap.keySet());
	}
}
