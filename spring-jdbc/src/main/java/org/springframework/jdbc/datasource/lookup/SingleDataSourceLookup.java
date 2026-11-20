/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.lookup;

import javax.sql.DataSource;

import org.springframework.util.Assert;

/**
 * An implementation of the DataSourceLookup that simply wraps a
 * single given DataSource, returned for any data source name.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SingleDataSourceLookup implements DataSourceLookup {

	private final DataSource dataSource;


	/**
	 * Create a new instance of the {@link SingleDataSourceLookup} class.
	 * @param dataSource the single {@link DataSource} to wrap
	 */
	public SingleDataSourceLookup(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.dataSource = dataSource;
	}


	@Override
	public DataSource getDataSource(String dataSourceName) {
		return this.dataSource;
	}

}
