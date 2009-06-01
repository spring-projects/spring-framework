
/**
 *
 * Simplification layer over JdbcTemplate for Java 5 and above.
 * 
 * <p>SimpleJdbcTemplate is a wrapper around JdbcTemplate that takes advantage
 * of varargs and autoboxing. It also offers only a subset of the methods
 * available on JdbcTemplate: Hence, it does not implement the JdbcOperations
 * interface or extend JdbcTemplate, but implements the dedicated
 * SimpleJdbcOperations interface.
 * 
 * <P>If you need the full power of Spring JDBC for less common operations,
 * use the <code>getJdbcOperations()</code> method of SimpleJdbcTemplate and work
 * with the returned classic template, or use a JdbcTemplate instance directly.
 *
 */
package org.springframework.jdbc.core.simple;

