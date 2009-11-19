/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cfg.Settings;

/**
 * Proxy for a Hibernate RegionFactory, delegating to a Spring-managed
 * RegionFactory instance, determined by LocalSessionFactoryBean's
 * "cacheRegionFactory" property.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see LocalSessionFactoryBean#setCacheRegionFactory
 */
public class LocalRegionFactoryProxy implements RegionFactory {

	private final RegionFactory regionFactory;


	public LocalRegionFactoryProxy() {
		RegionFactory rf = LocalSessionFactoryBean.getConfigTimeRegionFactory();
		// absolutely needs thread-bound RegionFactory to initialize
		if (rf == null) {
			throw new IllegalStateException("No Hibernate RegionFactory found - " +
			    "'cacheRegionFactory' property must be set on LocalSessionFactoryBean");
		}
		this.regionFactory = rf;
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
