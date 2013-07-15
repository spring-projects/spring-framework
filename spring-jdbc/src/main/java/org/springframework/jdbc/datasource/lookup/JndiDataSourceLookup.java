/*
 * Copyright 2002-2012 the original author or authors.
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
