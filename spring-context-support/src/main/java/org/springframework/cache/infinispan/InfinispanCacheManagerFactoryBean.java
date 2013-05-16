/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.cache.infinispan;

import java.io.IOException;
import java.io.InputStream;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * {@link FactoryBean} for a Infinispan {@link EmbeddedCacheManager}.
 *
 * <p>This class is intended to be used in conjunction with Spring XML configuration and
 * Infinispan XML configuration. While it does work with Spring Java configuration in
 * such cases you're likely better off instantiating the cache manager yourself and using
 * <a
 * href="https://docs.jboss.org/author/display/ISPN/Configuring+cache+programmatically">
 * Infinispan fluent configuration</a>.
 *
 * @author Philippe Marschall
 * @since 4.0
 */
public class InfinispanCacheManagerFactoryBean
		implements FactoryBean<EmbeddedCacheManager>, InitializingBean, DisposableBean {

	private Resource configLocation;

	private EmbeddedCacheManager cacheManager;


	/**
	 * Set the location of the Infinispan config file. A typical value is "/WEB-INF/infinispan.xml".
	 * @see org.infinispan.manager.DefaultCacheManager#DefaultCacheManager(java.io.InputStream)
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.configLocation != null) {
			InputStream configurationStream = this.configLocation.getInputStream();
			try {
				this.cacheManager = new DefaultCacheManager(configurationStream, true);
			} finally {
				configurationStream.close();
			}
		} else {
			this.cacheManager = new DefaultCacheManager(true);
		}
	}


	@Override
	public EmbeddedCacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<? extends EmbeddedCacheManager> getObjectType() {
		return this.cacheManager != null ? this.cacheManager.getClass() : EmbeddedCacheManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		this.cacheManager.stop();
	}
}
