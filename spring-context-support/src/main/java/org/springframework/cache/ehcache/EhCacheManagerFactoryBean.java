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

package org.springframework.cache.ehcache;

import java.io.IOException;
import java.io.InputStream;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * {@link FactoryBean} that exposes an EhCache {@link net.sf.ehcache.CacheManager}
 * instance (independent or shared), configured from a specified config location.
 *
 * <p>If no config location is specified, a CacheManager will be configured from
 * "ehcache.xml" in the root of the class path (that is, default EhCache initialization
 * - as defined in the EhCache docs - will apply).
 *
 * <p>Setting up a separate EhCacheManagerFactoryBean is also advisable when using
 * EhCacheFactoryBean, as it provides a (by default) independent CacheManager instance
 * and cares for proper shutdown of the CacheManager. EhCacheManagerFactoryBean is
 * also necessary for loading EhCache configuration from a non-default config location.
 *
 * <p>Note: As of Spring 4.0, Spring's EhCache support requires EhCache 2.1 or higher.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 1.1.1
 * @see #setConfigLocation
 * @see #setShared
 * @see EhCacheFactoryBean
 * @see net.sf.ehcache.CacheManager
 */
public class EhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private Resource configLocation;

	private boolean shared = false;

	private String cacheManagerName;

	private CacheManager cacheManager;


	/**
	 * Set the location of the EhCache config file. A typical value is "/WEB-INF/ehcache.xml".
	 * <p>Default is "ehcache.xml" in the root of the class path, or if not found,
	 * "ehcache-failsafe.xml" in the EhCache jar (default EhCache initialization).
	 * @see net.sf.ehcache.CacheManager#create(java.io.InputStream)
	 * @see net.sf.ehcache.CacheManager#CacheManager(java.io.InputStream)
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * Set whether the EhCache CacheManager should be shared (as a singleton at the VM level)
	 * or independent (typically local within the application). Default is "false", creating
	 * an independent instance.
	 * @see net.sf.ehcache.CacheManager#create()
	 * @see net.sf.ehcache.CacheManager#CacheManager()
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}

	/**
	 * Set the name of the EhCache CacheManager (if a specific name is desired).
	 * @see net.sf.ehcache.CacheManager#setName(String)
	 */
	public void setCacheManagerName(String cacheManagerName) {
		this.cacheManagerName = cacheManagerName;
	}


	@Override
	public void afterPropertiesSet() throws IOException, CacheException {
		logger.info("Initializing EhCache CacheManager");
		InputStream is = (this.configLocation != null ? this.configLocation.getInputStream() : null);
		try {
			Configuration configuration = (is != null ?
					ConfigurationFactory.parseConfiguration(is) : ConfigurationFactory.parseConfiguration());
			if (this.cacheManagerName != null) {
				configuration.setName(this.cacheManagerName);
			}
			this.cacheManager = (this.shared ? CacheManager.create(configuration) : new CacheManager(configuration));
		}
		finally {
			if (is != null) {
				is.close();
			}
		}
	}


	@Override
	public CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<? extends CacheManager> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		logger.info("Shutting down EhCache CacheManager");
		this.cacheManager.shutdown();
	}

}
