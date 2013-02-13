
/**
 *
 * The classes in this package make JDBC easier to use and
 * reduce the likelihood of common errors. In particular, they:
 * <ul>
 * <li>Simplify error handling, avoiding the need for try/catch/final
 * blocks in application code.
 * <li>Present exceptions to application code in a generic hierarchy of
 * unchecked exceptions, enabling applications to catch data access
 * exceptions without being dependent on JDBC, and to ignore fatal
 * exceptions there is no value in catching.
 * <li>Allow the implementation of error handling to be modified
 * to target different RDBMSes without introducing proprietary
 * dependencies into application code.
 * </ul>
 *
 * <p>This package and related packages are discussed in Chapter 9 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 *
 */
package org.springframework.jdbc;

