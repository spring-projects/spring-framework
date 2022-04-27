/**
 * Support classes for the JDBC framework, used by the classes in the
 * jdbc.core and jdbc.object packages. Provides a translator from
 * SQLExceptions Spring's generic DataAccessExceptions.
 *
 * <p>Can be used independently, for example in custom JDBC access code,
 * or in JDBC-based O/R mapping layers.
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.support;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
