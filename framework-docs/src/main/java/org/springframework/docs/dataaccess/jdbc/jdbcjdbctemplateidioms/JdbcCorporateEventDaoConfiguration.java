package org.springframework.docs.dataaccess.jdbc.jdbcjdbctemplateidioms;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JdbcCorporateEventDaoConfiguration {

	// tag::snippet[]
	@Bean
	JdbcCorporateEventDao corporateEventDao(DataSource dataSource) {
		return new JdbcCorporateEventDao(dataSource);
	}

	@Bean(destroyMethod = "close")
	BasicDataSource dataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
		dataSource.setUrl("jdbc:hsqldb:hsql://localhost:");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		return dataSource;
	}
	// end::snippet[]

}
