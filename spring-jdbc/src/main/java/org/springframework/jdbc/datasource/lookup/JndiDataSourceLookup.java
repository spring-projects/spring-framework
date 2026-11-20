/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.lookup;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.jndi.JndiLocatorSupport;

/**
 * JNDI-based {@link DataSourceLookup} implementation.
 *
 * <p>For specific JNDI configuration, it is recommended to configure
 * the "jndiEnvironment"/"jndiTemplate" properties.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setJndiEnvironment
 * @see #setJndiTemplate
 */
public class JndiDataSourceLookup extends JndiLocatorSupport implements DataSourceLookup {

	public JndiDataSourceLookup() {
		setResourceRef(true);
	}

	@Override
	public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
		try {
			return lookup(dataSourceName, DataSource.class);
		}
		catch (NamingException ex) {
			throw new DataSourceLookupFailureException(
					"Failed to look up JNDI DataSource with name '" + dataSourceName + "'", ex);
		}
	}

}
