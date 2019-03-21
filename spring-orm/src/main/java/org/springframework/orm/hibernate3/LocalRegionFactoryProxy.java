/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.hibernate3;

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

/**
 * Proxy for a Hibernate RegionFactory, delegating to a Spring-managed
 * RegionFactory instance, determined by LocalSessionFactoryBean's
 * "cacheRegionFactory" property.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see LocalSessionFactoryBean#setCacheRegionFactory
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
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


	@Override
	public void start(Settings settings, Properties properties) throws CacheException {
		this.regionFactory.start(settings, properties);
	}

	@Override
	public void stop() {
		this.regionFactory.stop();
	}

	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		return this.regionFactory.isMinimalPutsEnabledByDefault();
	}

	@Override
	public AccessType getDefaultAccessType() {
		return this.regionFactory.getDefaultAccessType();
	}

	@Override
	public long nextTimestamp() {
		return this.regionFactory.nextTimestamp();
	}

	@Override
	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {

		return this.regionFactory.buildEntityRegion(regionName, properties, metadata);
	}

	@Override
	public CollectionRegion buildCollectionRegion(String regionName, Properties properties,
			CacheDataDescription metadata) throws CacheException {

		return this.regionFactory.buildCollectionRegion(regionName, properties, metadata);
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties)
			throws CacheException {

		return this.regionFactory.buildQueryResultsRegion(regionName, properties);
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties)
			throws CacheException {

		return this.regionFactory.buildTimestampsRegion(regionName, properties);
	}

}
