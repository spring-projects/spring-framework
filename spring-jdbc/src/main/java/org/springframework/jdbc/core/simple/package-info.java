/**
 * Simplification layer for common JDBC interactions.
 *
 * <p>{@code JdbcClient} provides a fluent API for JDBC query and update operations,
 * supporting JDBC-style positional as well as Spring-style named parameters.
 *
 * <p>{@code SimpleJdbcInsert} and {@code SimpleJdbcCall} take advantage of database
 * meta-data provided by the JDBC driver to simplify the application code. Much of the
 * parameter specification becomes unnecessary since it can be looked up in the meta-data.
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.core.simple;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
