/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	/** @since 2.5.5 */
	H2,

	/** @since 5.1 */
	HANA,

	HSQL,

	INFORMIX,

	MYSQL,

	ORACLE,

	POSTGRESQL,

	SQL_SERVER,

	SYBASE

}
