/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.vendor;

/**
 * Enumeration for common database platforms. Allows strong typing of database type
 * and portable configuration between JpaVendorDialect implementations.
 *
 * <p>If a given PersistenceProvider supports a database not listed here,
 * the strategy class can still be specified using the fully-qualified class name.
 * This enumeration is merely a convenience. The database products listed here
 * are the same as those explicitly supported for Spring JDBC exception translation
 * in {@code sql-error-codes.xml}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see AbstractJpaVendorAdapter#setDatabase
 */
public enum Database {

	DEFAULT,

	DB2,

	DERBY,

	H2,

	HSQL,

	INFORMIX,

	MYSQL,

	ORACLE,

	POSTGRESQL,

	SQL_SERVER,

	SYBASE

}
