/*
 * Copyright 2010-2011 the original author or authors.
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

package org.springframework.cache.concurrent;

import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

/**
 * Factory bean for easy configuration of {@link ConcurrentMapCache} through Spring.
 * 
 * @author Costin Leau
 */
public class ConcurrentMapCacheFactoryBean implements FactoryBean<ConcurrentMapCache>, BeanNameAware,
		InitializingBean {

	private String name = "";
	private ConcurrentMapCache cache;

	private ConcurrentMap store;

	public void afterPropertiesSet() {
		cache = (store == null ? new ConcurrentMapCache(name) : new ConcurrentMapCache(store, name, true));
	}

	public ConcurrentMapCache getObject() throws Exception {
		return cache;
	}

	public Class<?> getObjectType() {
		return (cache != null ? cache.getClass() : ConcurrentMapCache.class);
	}

	public boolean isSingleton() {
		return true;
	}

	public void setBeanName(String beanName) {
		if (!StringUtils.hasText(name)) {
			setName(beanName);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStore(ConcurrentMap store) {
		this.store = store;
	}
}