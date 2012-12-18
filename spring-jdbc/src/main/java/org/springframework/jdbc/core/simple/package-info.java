
/**
 *
 * Simplification layer over JdbcTemplate for Java 5 and above.
 *
 * <p><code>SimpleJdbcInsert</code> and <code>SimpleJdbcCall</code> are classes that takes advantage
 * of database metadata provided by the JDBC driver to simplify the application code. Much of the
 * parameter specification becomes unnecessary since it can be looked up in the metadata.
 *
 * Note: The <code>SimpleJdbcOperations</code> and <code>SimpleJdbcTemplate</code>, which provides a wrapper
 * around JdbcTemplate to take advantage of Java 5 features like generics, varargs and autoboxing, is now deprecated
 * since Spring 3.1. All functionality is now available in the <code>JdbcOperations</code> and
 * <code>NamedParametersOperations</code> respectively.
 *
 */
package org.springframework.jdbc.core.simple;

