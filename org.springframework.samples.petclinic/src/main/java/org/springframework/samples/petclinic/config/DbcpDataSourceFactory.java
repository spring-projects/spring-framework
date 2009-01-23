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
package org.springframework.samples.petclinic.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

/**
 * A factory that creates a data source fit for use in a system environment. Creates a DBCP simple data source 
 * from the provided connection properties.
 *
 * This factory returns a fully-initialized DataSource implementation. When the DataSource is returned, callers are
 * guaranteed that the database schema and data will have been loaded by that time.
 *
 * Is a FactoryBean, for exposing the fully-initialized DataSource as a Spring bean. See {@link #getObject()}.
 * 
 * @author Chris Beams
 * @author Scott Andrews
 */
public class DbcpDataSourceFactory implements FactoryBean<DataSource>, DisposableBean {

    // configurable properties

    private String driverClassName;
    
    private String url;
    
    private String username;
    
    private String password;
    
    private boolean populate;

    private Resource schemaLocation;

    private Resource dataLocation;

    private Resource dropLocation;

    /**
     * The object created by this factory.
     */
    private BasicDataSource dataSource;

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    /**
     * The data source connection URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The data source username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     *The data source password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Indicates that the data base should be populated from the schema and data locations
     */
    public void setPopulate(boolean populate) {
        this.populate = populate;
    }

    /**
     * Sets the location of the file containing the schema DDL to export to the database.
     * @param schemaLocation the location of the database schema DDL
     */
    public void setSchemaLocation(Resource schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    /**
     * Sets the location of the file containing the data to load into the database.
     * @param testDataLocation the location of the data file
     */
    public void setDataLocation(Resource testDataLocation) {
        this.dataLocation = testDataLocation;
    }

    /**
     * Sets the location of the file containing the drop scripts for the database.
     * @param testDataLocation the location of the data file
     */
    public void setDropLocation(Resource testDropLocation) {
        this.dropLocation = testDropLocation;
    }

    // implementing FactoryBean

    // this method is called by Spring to expose the DataSource as a bean
    public DataSource getObject() throws Exception {
        if (dataSource == null) {
            initDataSource();
        }
        return dataSource;
    }

    public Class<DataSource> getObjectType() {
        return DataSource.class;
    }

    public boolean isSingleton() {
        return true;
    }

    // implementing DisposableBean

    public void destroy() throws Exception {
    	dataSource.close();
    }

    // internal helper methods

    // encapsulates the steps involved in initializing the data source: creating it, and populating it
    private void initDataSource() {
        // create the database source first
        this.dataSource = createDataSource();

        if (this.populate) {
        	// now populate the database by loading the schema and data
        	populateDataSource();
        }
    }

    private BasicDataSource createDataSource() {
    	BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(this.driverClassName);
        dataSource.setUrl(this.url);
        dataSource.setUsername(this.username);
        dataSource.setPassword(this.password);
        return dataSource;
    }

    private void populateDataSource() {
        DatabasePopulator populator = new DatabasePopulator(dataSource);
        if (dropLocation != null) {
            try {
        		populator.populate(this.dropLocation);
            } 
            catch (Exception e) {
               	// ignore
            }
        }
        populator.populate(this.schemaLocation);
        populator.populate(this.dataLocation);
    }

    /**
     * Populates a in memory data source with data.
     */
    private class DatabasePopulator {

        private DataSource dataSource;

        /**
         * Creates a new database populator.
         * @param dataSource the data source that will be populated.
         */
        public DatabasePopulator(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Populate the database executing the statements in the provided resource against the database
         * @param sqlFile spring resource containing SQL to run against the db
         */
        public void populate(Resource sqlFile) {
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
                try {
                    String sql = parseSqlIn(sqlFile);
                    executeSql(sql, connection);
                } catch (IOException e) {
                    throw new RuntimeException("I/O exception occurred accessing the database schema file", e);
                } catch (SQLException e) {
                    throw new RuntimeException("SQL exception occurred exporting database schema", e);
                }
            } catch (SQLException e) {
                throw new RuntimeException("SQL exception occurred acquiring connection", e);
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }

        // utility method to read a .sql txt input stream
        private String parseSqlIn(Resource resource) throws IOException {
            InputStream is = null;
            try {
                is = resource.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                StringWriter sw = new StringWriter();
                BufferedWriter writer = new BufferedWriter(sw);

                for (int c=reader.read(); c != -1; c=reader.read()) {
                    writer.write(c);
                }
                writer.flush();
                return sw.toString();

            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        // utility method to run the parsed sql
        private void executeSql(String sql, Connection connection) throws SQLException {
            Statement statement = connection.createStatement();
            statement.execute(sql);
        }
    }

}
