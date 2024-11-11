package org.springframework.docs.dataaccess.jdbc.jdbcjdbctemplateidioms;

import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// tag::snippet[]
@Configuration
@ComponentScan("org.example.jdbc")
public class JdbcCorporateEventRepositoryConfiguration {

	@Bean(destroyMethod = "close")
	BasicDataSource dataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
		dataSource.setUrl("jdbc:hsqldb:hsql://localhost:");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		return dataSource;
	}

}
// end::snippet[]
