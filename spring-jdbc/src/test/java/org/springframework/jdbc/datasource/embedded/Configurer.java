package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

import javax.sql.DataSource;

public class Configurer implements EmbeddedDatabaseConfigurer {

	private boolean configureCalled;
	private boolean shutdownCalled;
	private EmbeddedDatabaseConfigurer configurer;
	
	public static Configurer getHsqldbConfigurer() {
		return new Configurer(EmbeddedDatabaseConfigurerFactory.getConfigurer(EmbeddedDatabaseType.HSQL));
	}
	
	public static Configurer getH2Configurer() {
		return new Configurer(EmbeddedDatabaseConfigurerFactory.getConfigurer(EmbeddedDatabaseType.H2));
	}
	
	public static Configurer getDerbyConfigurer() {
		return new Configurer(EmbeddedDatabaseConfigurerFactory.getConfigurer(EmbeddedDatabaseType.DERBY));
	}
	
	public Configurer(EmbeddedDatabaseConfigurer configurer) {
		this.configurer = configurer;
	}
	
	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		configurer.configureConnectionProperties(properties, databaseName);
		configureCalled = true;
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		configurer.shutdown(dataSource, databaseName);
		shutdownCalled = true;
	}
	
	
	public boolean isConfigureCalled() {
		return configureCalled;
	}
	
	
	public boolean isShutdownCalled() {
		return shutdownCalled;
	}


}