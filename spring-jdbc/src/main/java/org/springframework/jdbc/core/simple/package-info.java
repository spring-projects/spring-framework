/**
 * Simplification layer over JdbcTemplate for Java 5 and above.
 *
 * <p>{@code SimpleJdbcInsert} and {@code SimpleJdbcCall} are classes that takes advantage
 * of database metadata provided by the JDBC driver to simplify the application code. Much of the
 * parameter specification becomes unnecessary since it can be looked up in the metadata.
 */
package org.springframework.jdbc.core.simple;
