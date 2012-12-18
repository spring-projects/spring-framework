/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.datasource.lookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.util.Assert;

/**
 * Simple {@link DataSourceLookup} implementation that relies on a map for doing lookups.
 *
 * <p>Useful for testing environments or applications that need to match arbitrary
 * {@link String} names to target {@link DataSource} objects.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 */
public class MapDataSourceLookup implements DataSourceLookup {

	private final Map<String, DataSource> dataSources = new HashMap<String, DataSource>(4);


	/**
	 * Create a new instance of the {@link MapDataSourceLookup} class.
	 */
	public MapDataSourceLookup() {
	}

	/**
	 * Create a new instance of the {@link MapDataSourceLookup} class.
	 * @param dataSources the {@link Map} of {@link DataSource DataSources}; the keys
	 * are {@link String Strings}, the values are actual {@link DataSource} instances.
	 */
	public MapDataSourceLookup(Map<String, DataSource> dataSources) {
		setDataSources(dataSources);
	}

	/**
	 * Create a new instance of the {@link MapDataSourceLookup} class.
	 * @param dataSourceName the name under which the supplied {@link DataSource} is to be added
	 * @param dataSource the {@link DataSource} to be added
	 */
	public MapDataSourceLookup(String dataSourceName, DataSource dataSource) {
		addDataSource(dataSourceName, dataSource);
	}


	/**
	 * Set the {@link Map} of {@link DataSource DataSources}; the keys
	 * are {@link String Strings}, the values are actual {@link DataSource} instances.
	 * <p>If the supplied {@link Map} is {@code null}, then this method
	 * call effectively has no effect.
	 * @param dataSources said {@link Map} of {@link DataSource DataSources}
	 */
	public void setDataSources(Map<String, DataSource> dataSources) {
		if (dataSources != null) {
			this.dataSources.putAll(dataSources);
		}
	}

	/**
	 * Get the {@link Map} of {@link DataSource DataSources} maintained by this object.
	 * <p>The returned {@link Map} is {@link Collections#unmodifiableMap(java.util.Map) unmodifiable}.
	 * @return said {@link Map} of {@link DataSource DataSources} (never {@code null})
	 */
	public Map<String, DataSource> getDataSources() {
		return Collections.unmodifiableMap(this.dataSources);
	}

	/**
	 * Add the supplied {@link DataSource} to the map of {@link DataSource DataSources}
	 * maintained by this object.
	 * @param dataSourceName the name under which the supplied {@link DataSource} is to be added
	 * @param dataSource the {@link DataSource} to be so added
	 */
	public void addDataSource(String dataSourceName, DataSource dataSource) {
		Assert.notNull(dataSourceName, "DataSource name must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		this.dataSources.put(dataSourceName, dataSource);
	}

	public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
		Assert.notNull(dataSourceName, "DataSource name must not be null");
		DataSource dataSource = this.dataSources.get(dataSourceName);
		if (dataSource == null) {
			throw new DataSourceLookupFailureException(
					"No DataSource with name '" + dataSourceName + "' registered");
		}
		return dataSource;
	}

}
