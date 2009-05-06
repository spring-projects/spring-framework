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
package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A subclass of {@link EmbeddedDatabaseFactory} that implements {@link FactoryBean} for registration as a Spring bean.
 * Returns the actual {@link DataSource} that provides connectivity to the embedded database to Spring.
 * The target DataSource is returned instead of a {@link EmbeddedDatabase} proxy since the FactoryBean will manage the initialization and destruction lifecycle of the database instance.
 * Implements DisposableBean to shutdown the embedded database when the managing Spring container is shutdown.
 * 
 * @author Keith Donald
 */
public class EmbeddedDatabaseFactoryBean extends EmbeddedDatabaseFactory implements FactoryBean<DataSource>, InitializingBean, DisposableBean {

	public void afterPropertiesSet() throws Exception {
		initDatabase();
	}

	public DataSource getObject() throws Exception {
		return getDataSource();
	}

	public Class<? extends DataSource> getObjectType() {
		return DataSource.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void destroy() throws Exception {
		shutdownDatabase();
	}	

}
