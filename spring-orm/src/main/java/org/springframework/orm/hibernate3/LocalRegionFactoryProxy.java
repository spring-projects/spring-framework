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

package org.springframework.orm.hibernate3;

import java.lang.reflect.Method;
import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;

import org.springframework.util.ReflectionUtils;

/**
 * Proxy for a Hibernate RegionFactory, delegating to a Spring-managed
 * RegionFactory instance, determined by LocalSessionFactoryBean's
 * "cacheRegionFactory" property.
 *
 * <p>Compatible with Hibernate 3.3 as well as Hibernate 3.5's version
 * of the RegionFactory SPI.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see LocalSessionFactoryBean#setCacheRegionFactory
 */
public class LocalRegionFactoryProxy implements RegionFactory {

	private final RegionFactory regionFactory;


	/**
	 * Standard constructor.
	 */
	public LocalRegionFactoryProxy() {
		RegionFactory rf = (RegionFactory) LocalSessionFactoryBean.getConfigTimeRegionFactory();
		// absolutely needs thread-bound RegionFactory to initialize
		if (rf == null) {
			throw new IllegalStateException("No Hibernate RegionFactory found - " +
				"'cacheRegionFactory' property must be set on LocalSessionFactoryBean");
		}
		this.regionFactory = rf;
	}

	/**
	 * Properties constructor: not used by this class or formally required,
	 * but enforced by Hibernate when reflectively instantiating a RegionFactory.
	 */
	public LocalRegionFactoryProxy(Properties properties) {
		this();
	}


	public void start(Settings settings, Properties properties) throws CacheException {
		this.regionFactory.start(settings, properties);
	}

	public void stop() {
		this.regionFactory.stop();
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return this.regionFactory.isMinimalPutsEnabledByDefault();
	}

	public AccessType getDefaultAccessType() {
		try {
			Method method = RegionFactory.class.getMethod("getDefaultAccessType");
			return (AccessType) ReflectionUtils.invokeMethod(method, this.regionFactory);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("getDefaultAccessType requires Hibernate 3.5+");
		}
	}

	public long nextTimestamp() {
		return this.regionFactory.nextTimestamp();
	}

	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {

		return this.regionFactory.buildEntityRegion(regionName, properties, metadata);
	}

	public CollectionRegion buildCollectionRegion(String regionName, Properties properties,
			CacheDataDescription metadata) throws CacheException {

		return this.regionFactory.buildCollectionRegion(regionName, properties, metadata);
	}

	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties)
			throws CacheException {

		return this.regionFactory.buildQueryResultsRegion(regionName, properties);
	}

	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties)
			throws CacheException {

		return this.regionFactory.buildTimestampsRegion(regionName, properties);
	}

}
