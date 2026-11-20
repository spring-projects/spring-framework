/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbcdatasource;

import java.beans.PropertyVetoException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ComboPooledDataSourceConfiguration {

	// tag::snippet[]
	@Bean(destroyMethod = "close")
	ComboPooledDataSource dataSource() throws PropertyVetoException {
		ComboPooledDataSource dataSource = new ComboPooledDataSource();
		dataSource.setDriverClass("org.hsqldb.jdbcDriver");
		dataSource.setJdbcUrl("jdbc:hsqldb:hsql://localhost:");
		dataSource.setUser("sa");
		dataSource.setPassword("");
		return dataSource;
	}
	// end::snippet[]

}
