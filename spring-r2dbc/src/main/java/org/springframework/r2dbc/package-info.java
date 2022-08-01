/**
 * The classes in this package make R2DBC easier to use and
 * reduce the likelihood of common errors. In particular, they:
 * <ul>
 * <li>Simplify error handling, avoiding the need for resource management
 * blocks in application code.
 * <li>Present exceptions to application code in a generic hierarchy of
 * unchecked exceptions, enabling applications to catch data access
 * exceptions without being dependent on R2DBC, and to ignore fatal
 * exceptions there is no value in catching.
 * <li>Allow the implementation of error handling to be modified
 * to target different RDBMSes without introducing proprietary
 * dependencies into application code.
 * </ul>
 */
@NonNullApi
@NonNullFields
package org.springframework.r2dbc;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
