/**
 * The classes in this package represent RDBMS queries, updates, and stored
 * procedures as threadsafe, reusable objects. This approach is modeled by JDO,
 * although objects returned by queries are of course "disconnected" from the
 * database.
 *
 * <p>This higher-level JDBC abstraction depends on the lower-level
 * abstraction in the {@code org.springframework.jdbc.core} package.
 * Exceptions thrown are as in the {@code org.springframework.dao} package,
 * meaning that code using this package does not need to implement JDBC or
 * RDBMS-specific error handling.
 *
 * <p>This package and related packages are discussed in Chapter 9 of
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.object;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
