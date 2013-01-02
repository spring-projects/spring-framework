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

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;

/**
 * A subclass of {@link EmbeddedDatabaseFactory} that implements {@link FactoryBean} for registration as a Spring bean.
 * Returns the actual {@link DataSource} that provides connectivity to the embedded database to Spring.
 *
 * <p>The target DataSource is returned instead of a {@link EmbeddedDatabase} proxy since the FactoryBean
 * will manage the initialization and destruction lifecycle of the database instance.
 *
 * <p>Implements DisposableBean to shutdown the embedded database when the managing Spring container is shutdown.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class EmbeddedDatabaseFactoryBean extends EmbeddedDatabaseFactory
		implements FactoryBean<DataSource>, InitializingBean, DisposableBean {

	private DatabasePopulator databaseCleaner;


	/**
	 * Set a script execution to be run in the bean destruction callback,
	 * cleaning up the database and leaving it in a known state for others.
	 * @param databaseCleaner the database script executor to run on destroy
	 * @see #setDatabasePopulator
	 * @see org.springframework.jdbc.datasource.init.DataSourceInitializer#setDatabaseCleaner
	 */
	public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
		this.databaseCleaner = databaseCleaner;
	}

	public void afterPropertiesSet() {
		initDatabase();
	}

	public void destroy() {
		if (this.databaseCleaner != null) {
			DatabasePopulatorUtils.execute(this.databaseCleaner, getDataSource());
		}
		shutdownDatabase();
	}


	public DataSource getObject() {
		return getDataSource();
	}

	public Class<? extends DataSource> getObjectType() {
		return DataSource.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
