
/**
 *
 * JdbcTemplate variant with named parameter support.
 *
 * <p>NamedParameterJdbcTemplate is a wrapper around JdbcTemplate that adds
 * support for named parameter parsing. It does not implement the JdbcOperations
 * interface or extend JdbcTemplate, but implements the dedicated
 * NamedParameterJdbcOperations interface.
 *
 * <P>If you need the full power of Spring JDBC for less common operations, use
 * the <code>getJdbcOperations()</code> method of NamedParameterJdbcTemplate and
 * work with the returned classic template, or use a JdbcTemplate instance directly.
 *
 */
package org.springframework.jdbc.core.namedparam;

